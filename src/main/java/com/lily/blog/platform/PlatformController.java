package com.lily.blog.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 배포 플랫폼 검증 전용 엔드포인트.
 * 블루-그린 전환 확인 / 장애 주입 / 인스턴스 식별에 사용한다.
 */
@RestController
public class PlatformController {

    private static final Logger log = LoggerFactory.getLogger(PlatformController.class);
    private static final Instant STARTED_AT = Instant.now();

    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationAvailability availability;

    @Value("${app.version:dev}")
    private String version;

    @Value("${app.color:blue}")
    private String color;

    public PlatformController(ApplicationEventPublisher eventPublisher, ApplicationAvailability availability) {
        this.eventPublisher = eventPublisher;
        this.availability = availability;
    }

    /** 블루-그린 / 카나리 전환 시 어느 인스턴스가 응답했는지 확인 */
    @GetMapping("/version")
    public Map<String, Object> version() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", version);
        map.put("color", color);
        map.put("instance", hostname());
        map.put("startedAt", STARTED_AT.toString());
        map.put("readiness", availability.getReadinessState().name());
        return map;
    }

    /** 로드밸런서 트래픽 분산 확인용 - 어느 인스턴스로 갔는지만 반환 */
    @GetMapping("/whoami")
    public Map<String, String> whoami() {
        return Map.of("instance", hostname(), "color", color);
    }

    /** 장애 주입: 500 에러 발생 (자동 롤백 트리거 시연용) */
    @GetMapping("/chaos/error")
    public ResponseEntity<Void> chaosError() {
        log.error("chaos: forced application error triggered");
        throw new IllegalStateException("chaos: forced application error");
    }

    /** 장애 주입: 응답 지연 (타임아웃/헬스체크 실패 시연용) */
    @GetMapping("/chaos/slow")
    public Map<String, Object> chaosSlow(@RequestParam(defaultValue = "5000") long ms) throws InterruptedException {
        log.warn("chaos: sleeping {}ms", ms);
        Thread.sleep(ms);
        return Map.of("sleptMs", ms, "instance", hostname());
    }

    /** 장애 주입: readiness를 OUT_OF_SERVICE로 전환 (LB에서 제외되는지 확인) */
    @PostMapping("/chaos/unready")
    public Map<String, String> chaosUnready() {
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        log.error("chaos: readiness switched to REFUSING_TRAFFIC");
        return Map.of("readiness", "REFUSING_TRAFFIC", "instance", hostname());
    }

    /** readiness 복구 */
    @PostMapping("/chaos/ready")
    public Map<String, String> chaosReady() {
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
        log.info("chaos: readiness restored to ACCEPTING_TRAFFIC");
        return Map.of("readiness", "ACCEPTING_TRAFFIC", "instance", hostname());
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
