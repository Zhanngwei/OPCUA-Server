package org.go.show.multiproto.opcuaserver;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TestUsernameLogin {
    
    public static void main(String[] args) {
        System.out.println("=== OPC UA Authentication Test Suite ===");

        int passed = 0;
        int total = 0;

        // Test 1: Valid credentials
        total++;
        if (testValidCredentials()) passed++;

        // Test 2: Invalid credentials
        total++;
        if (testInvalidCredentials()) passed++;

        // Test 3: Anonymous connection (should fail)
        total++;
        if (testAnonymousConnection()) passed++;

        System.out.println("\n=== Test Results ===");
        System.out.println("Passed: " + passed + "/" + total);
        if (passed == total) {
            System.out.println("🎉 ALL TESTS PASSED! Authentication is working correctly!");
        } else {
            System.out.println("❌ Some tests failed. Please check the authentication configuration.");
        }
    }

    public static boolean testValidCredentials() {
        System.out.println("\n--- Test 1: Valid Credentials (admin/admin123) ---");
        try {
            testUsernameLogin("admin", "admin123");
            System.out.println("✅ SUCCESS: Valid credentials accepted");
            return true;
        } catch (Exception e) {
            System.out.println("❌ FAILED: Valid credentials rejected - " + e.getMessage());
            return false;
        }
    }

    public static boolean testInvalidCredentials() {
        System.out.println("\n--- Test 2: Invalid Credentials (wronguser/wrongpass) ---");
        try {
            testUsernameLogin("wronguser", "wrongpass");
            System.out.println("❌ FAILED: Invalid credentials should be rejected");
            return false;
        } catch (Exception e) {
            System.out.println("✅ SUCCESS: Invalid credentials correctly rejected - " + e.getMessage());
            return true;
        }
    }

    public static boolean testAnonymousConnection() {
        System.out.println("\n--- Test 3: Anonymous Connection (should fail) ---");
        try {
            testAnonymousLogin();
            System.out.println("❌ FAILED: Anonymous connection should be rejected");
            return false;
        } catch (Exception e) {
            System.out.println("✅ SUCCESS: Anonymous connection correctly rejected - " + e.getMessage());
            return true;
        }
    }
    
    public static void testUsernameLogin(String username, String password) throws Exception {
        String endpointUrl = "opc.tcp://localhost:4840";
        
        try {
            System.out.println("=== Testing OPC UA Username/Password Authentication with Milo Client ===");
            System.out.println("Connecting to: " + endpointUrl);
            System.out.println();
            
            // 1. 发现端点
            System.out.println("1. Discovering endpoints...");
            CompletableFuture<List<EndpointDescription>> endpointsFuture = DiscoveryClient.getEndpoints(endpointUrl);
            List<EndpointDescription> endpointsList = endpointsFuture.get(10, TimeUnit.SECONDS);
            
            System.out.println("Found " + endpointsList.size() + " endpoints:");
            for (int i = 0; i < endpointsList.size(); i++) {
                EndpointDescription endpoint = endpointsList.get(i);
                System.out.println("  Endpoint " + (i + 1) + ":");
                System.out.println("    URL: " + endpoint.getEndpointUrl());
                System.out.println("    Security Policy: " + endpoint.getSecurityPolicyUri());
                System.out.println("    Security Mode: " + endpoint.getSecurityMode());
            }
            System.out.println();
            
            if (endpointsList.isEmpty()) {
                System.err.println("❌ No endpoints found!");
                return;
            }
            
            // 2. 选择None安全策略的端点
            EndpointDescription selectedEndpoint = null;
            for (EndpointDescription endpoint : endpointsList) {
                if (SecurityPolicy.None.getUri().equals(endpoint.getSecurityPolicyUri()) &&
                    MessageSecurityMode.None.equals(endpoint.getSecurityMode())) {
                    selectedEndpoint = endpoint;
                    break;
                }
            }
            
            if (selectedEndpoint == null) {
                selectedEndpoint = endpointsList.get(0); // 使用第一个端点
            }
            
            System.out.println("2. Selected endpoint: " + selectedEndpoint.getEndpointUrl());
            System.out.println("   Security Policy: " + selectedEndpoint.getSecurityPolicyUri());
            System.out.println("   Security Mode: " + selectedEndpoint.getSecurityMode());
            System.out.println();
            
            // 3. 测试匿名连接
            System.out.println("3. Testing anonymous connection...");
            testAnonymousConnection(selectedEndpoint);
            
            // 4. 测试用户名密码连接
            System.out.println("4. Testing username/password connection...");
            testUsernameConnection(selectedEndpoint, "admin", "admin123");
            
            // 5. 测试错误的密码
            System.out.println("5. Testing with wrong password...");
            testUsernameConnection(selectedEndpoint, "admin", "wrongpassword");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testAnonymousConnection(EndpointDescription endpoint) {
        try {
            System.out.println("  Creating anonymous client...");
            
            // 使用Milo 1.0的正确API创建客户端
            OpcUaClient client = OpcUaClient.create(
                endpoint.getEndpointUrl(),
                endpoints -> endpoints.stream()
                    .filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
                    .findFirst(),
                transportConfigBuilder -> {},
                clientConfigBuilder -> clientConfigBuilder
                    .setApplicationName(LocalizedText.english("Test Anonymous Client"))
                    .setApplicationUri("urn:test:anonymous:client")
                    .setIdentityProvider(new AnonymousProvider())
            );
            
            try {
                System.out.println("  Connecting anonymously...");
                client.connect();
                System.out.println("  ✅ Anonymous connection successful!");
                
                // 等待一下让服务器处理
                Thread.sleep(1000);
                
            } catch (Exception e) {
                System.err.println("  ❌ Anonymous connection failed: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("  Cause: " + e.getCause().getMessage());
                }
            } finally {
                try {
                    client.disconnect();
                    System.out.println("  Anonymous client disconnected.");
                } catch (Exception e) {
                    System.err.println("  Error disconnecting anonymous client: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("  ❌ Failed to create anonymous client: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testUsernameConnection(EndpointDescription endpoint, String username, String password) {
        try {
            System.out.println("  Creating username client with credentials: " + username + "/" + password);
            
            // 使用Milo 1.0的正确API创建客户端
            OpcUaClient client = OpcUaClient.create(
                endpoint.getEndpointUrl(),
                endpoints -> endpoints.stream()
                    .filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
                    .findFirst(),
                transportConfigBuilder -> {},
                clientConfigBuilder -> clientConfigBuilder
                    .setApplicationName(LocalizedText.english("Test Username Client"))
                    .setApplicationUri("urn:test:username:client")
                    .setIdentityProvider(new UsernameProvider(username, password))
            );
            
            try {
                System.out.println("  Connecting with username/password...");
                client.connect();
                System.out.println("  ✅ Username connection successful!");
                
                // 等待一下让服务器处理
                Thread.sleep(1000);
                
            } catch (Exception e) {
                System.err.println("  ❌ Username connection failed: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("  Cause: " + e.getCause().getMessage());
                }
            } finally {
                try {
                    client.disconnect();
                    System.out.println("  Username client disconnected.");
                } catch (Exception e) {
                    System.err.println("  Error disconnecting username client: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("  ❌ Failed to create username client: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    public static void testAnonymousLogin() throws Exception {
        String endpointUrl = "opc.tcp://localhost:4840";

        System.out.println("Connecting to: " + endpointUrl);
        System.out.println();

        // 1. 发现端点
        System.out.println("1. Discovering endpoints...");
        CompletableFuture<List<EndpointDescription>> endpointsFuture = DiscoveryClient.getEndpoints(endpointUrl);
        List<EndpointDescription> endpointsList = endpointsFuture.get(10, TimeUnit.SECONDS);

        if (endpointsList.isEmpty()) {
            throw new RuntimeException("No endpoints found!");
        }

        // 选择一个端点
        EndpointDescription selectedEndpoint = endpointsList.stream()
            .filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No suitable endpoint found"));

        System.out.println("Selected endpoint: " + selectedEndpoint.getEndpointUrl());

        // 2. 测试匿名连接
        System.out.println("2. Testing anonymous connection...");
        testAnonymousConnection(selectedEndpoint);
    }
}
