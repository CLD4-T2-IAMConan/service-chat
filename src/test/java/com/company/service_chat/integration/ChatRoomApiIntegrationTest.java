package com.company.service_chat.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Chat Room API 통합 테스트
 *
 * Testcontainers를 사용하여 실제 MySQL과 연동 테스트
 *
 * 테스트 범위:
 * - Health check
 * - 채팅방 생성
 * - 채팅방 목록 조회
 * - 메시지 읽음 처리
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@DisplayName("Chat Room API 통합 테스트")
class ChatRoomApiIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    @DisplayName("Health check - 성공")
    void healthCheck_success() {
        given()
        .when()
            .get("/api/health")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("UP"))
            .body("data.service", equalTo("service_chat"));
    }

    @Test
    @DisplayName("채팅방 생성 - 성공")
    void createChatRoom_success() {
        Map<String, Object> request = Map.of(
            "ticketId", 1L,
            "buyerId", 100L
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/chat/rooms")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.chatroomId", notNullValue())
            .body("data.ticketId", equalTo(1))
            .body("data.roomStatus", notNullValue());
    }

    @Test
    @DisplayName("채팅방 목록 조회 - 성공")
    void getChatRooms_success() {
        // Given - 채팅방 생성
        Map<String, Object> createRequest = Map.of(
            "ticketId", 2L,
            "buyerId", 200L
        );

        given()
            .contentType(ContentType.JSON)
            .body(createRequest)
        .when()
            .post("/chat/rooms");

        // When & Then - 목록 조회
        given()
            .queryParam("userId", 200L)
        .when()
            .get("/chat/rooms")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("메시지 목록 조회 - 성공")
    void getChatMessages_success() {
        // Given - 채팅방 생성
        Map<String, Object> createRequest = Map.of(
            "ticketId", 3L,
            "buyerId", 300L
        );

        Integer chatroomId = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
        .when()
            .post("/chat/rooms")
        .then()
            .statusCode(200)
            .extract()
            .path("data.chatroomId");

        // When & Then - 메시지 조회
        given()
        .when()
            .get("/chat/rooms/" + chatroomId + "/messages")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    @DisplayName("채팅방 삭제 - 성공")
    void deleteChatRoom_success() {
        // Given - 채팅방 생성
        Map<String, Object> createRequest = Map.of(
            "ticketId", 4L,
            "buyerId", 400L
        );

        Integer chatroomId = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
        .when()
            .post("/chat/rooms")
        .then()
            .statusCode(200)
            .extract()
            .path("data.chatroomId");

        // When & Then - 채팅방 삭제
        given()
            .queryParam("userId", 400L)
        .when()
            .delete("/chat/rooms/" + chatroomId)
        .then()
            .statusCode(200)
            .body("success", equalTo(true));
    }
}
