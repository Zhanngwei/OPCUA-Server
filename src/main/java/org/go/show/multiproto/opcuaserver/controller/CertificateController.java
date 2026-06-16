package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.model.Certificate;
import org.go.show.multiproto.opcuaserver.service.CertificatePersistenceTest;
import org.go.show.multiproto.opcuaserver.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 证书管理控制器
 */
@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {
    
    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificatePersistenceTest persistenceTest;
    
    /**
     * 获取所有证书
     */
    @GetMapping
    public ResponseEntity<List<Certificate>> getAllCertificates() {
        try {
            List<Certificate> certificates = certificateService.getAllCertificates();
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据类型获取证书
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Certificate>> getCertificatesByType(@PathVariable String type) {
        try {
            List<Certificate> certificates = certificateService.getCertificatesByType(type);
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据ID获取证书详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificateById(@PathVariable Long id) {
        try {
            Certificate certificate = certificateService.getCertificateById(id);
            if (certificate != null) {
                return ResponseEntity.ok(certificate);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 信任证书
     */
    @PostMapping("/{id}/trust")
    public ResponseEntity<Map<String, Object>> trustCertificate(@PathVariable Long id) {
        try {
            boolean success = certificateService.trustCertificate(id);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Certificate trusted successfully" : "Failed to trust certificate"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error trusting certificate: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 拒绝证书
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectCertificate(@PathVariable Long id) {
        try {
            boolean success = certificateService.rejectCertificate(id);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Certificate rejected successfully" : "Failed to reject certificate"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error rejecting certificate: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 删除证书
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCertificate(@PathVariable Long id) {
        try {
            boolean success = certificateService.deleteCertificate(id);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Certificate deleted successfully" : "Failed to delete certificate"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error deleting certificate: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重新创建自有证书
     */
    @PostMapping("/{id}/recreate")
    public ResponseEntity<Map<String, Object>> recreateOwnCertificate(@PathVariable Long id) {
        try {
            boolean success = certificateService.recreateOwnCertificate(id);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Certificate recreated successfully" : "Failed to recreate certificate"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error recreating certificate: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 刷新证书列表
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCertificates() {
        try {
            certificateService.refreshCertificates();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Certificate list refreshed successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error refreshing certificates: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取证书统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCertificateStats() {
        try {
            Map<String, Object> stats = certificateService.getCertificateStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取受信任的证书
     */
    @GetMapping("/trusted")
    public ResponseEntity<List<Certificate>> getTrustedCertificates() {
        try {
            List<Certificate> certificates = certificateService.getTrustedCertificates();
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取即将过期的证书
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<Certificate>> getExpiringCertificates(@RequestParam(defaultValue = "30") int days) {
        try {
            List<Certificate> certificates = certificateService.getExpiringCertificates(days);
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取已过期的证书
     */
    @GetMapping("/expired")
    public ResponseEntity<List<Certificate>> getExpiredCertificates() {
        try {
            List<Certificate> certificates = certificateService.getExpiredCertificates();
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据应用程序URI查找证书
     */
    @GetMapping("/by-uri/{applicationUri}")
    public ResponseEntity<Certificate> getCertificateByApplicationUri(@PathVariable String applicationUri) {
        try {
            Certificate certificate = certificateService.getCertificateByApplicationUri(applicationUri);
            if (certificate != null) {
                return ResponseEntity.ok(certificate);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 扫描并加载证书
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanCertificates() {
        try {
            certificateService.scanAndLoadCertificates();

            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Certificate scan completed successfully"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error scanning certificates: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 测试证书持久化功能
     */
    @PostMapping("/test-persistence")
    public ResponseEntity<Map<String, Object>> testPersistence() {
        try {
            boolean testResult = persistenceTest.runPersistenceTest();

            Map<String, Object> response = Map.of(
                "success", testResult,
                "message", testResult ? "Certificate persistence test passed" : "Certificate persistence test failed"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error running persistence test: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 打印证书统计信息到日志
     */
    @GetMapping("/print-stats")
    public ResponseEntity<Map<String, Object>> printCertificateStatistics() {
        try {
            persistenceTest.printCertificateStats();

            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Certificate statistics printed to log"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error getting certificate statistics: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
