package com.tuzki.mall.user;

import com.tuzki.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    /**
     *
     * @throws Exception
     */
    @Test
    void registerCreatesUserAndReturnsPublicUserInfo() throws Exception {
        long suffix = System.nanoTime();
        String username = "api_user_" + suffix;

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123",
                                  "nickname": "Api User",
                                  "phone": "139%s",
                                  "email": "api-user@example.com"
                                }
                                """.formatted(username, String.valueOf(suffix).substring(0, 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.nickname").value("Api User"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        String username = "duplicate_user_" + System.nanoTime();
        String requestBody = """
                {
                  "username": "%s",
                  "password": "password123",
                  "nickname": "Duplicate User"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username already exists"));
    }

    @Test
    void getUserReturnsPublicUserInfoWithoutPassword() throws Exception {
        String username = "query_user_" + System.nanoTime();

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123",
                                  "nickname": "Query User"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk());

        Long userId = userMapper.selectList(null).stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void getUserRejectsMissingUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("user not found"));
    }

    @Test
    void loginPersistsTokenAndLogoutInvalidatesToken() throws Exception {
        String username = "login_user_" + System.nanoTime();
        registerUser(username, "password123", "Login User");

        String token = mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("invalid login token"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String username = "wrong_password_user_" + System.nanoTime();
        registerUser(username, "password123", "Wrong Password User");

        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "bad-password"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username or password incorrect"));
    }

    private void registerUser(String username, String password, String nickname) throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(username, password, nickname)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
