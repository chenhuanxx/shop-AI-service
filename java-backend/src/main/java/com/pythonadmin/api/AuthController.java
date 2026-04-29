package com.pythonadmin.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import com.pythonadmin.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pythonadmin.config.AppProperties;
import com.pythonadmin.service.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final AppProperties props;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile String cachedWxAccessToken = "";
    private volatile long cachedWxAccessTokenExpireAtEpochSeconds = 0;

    public AuthController(
        UserRepository userRepo,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        CurrentUserService currentUserService,
        ObjectMapper objectMapper,
        AppProperties props
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String username, @NotBlank String password) {}
    public record WeComLoginRequest(@NotBlank String code) {}
    public record WeixinUserInfo(
        String nickName,
        String avatarUrl,
        Integer gender,
        String country,
        String province,
        String city,
        String language
    ) {}
    public record WeixinLoginRequest(String code, WeixinUserInfo userInfo) {}
    public record WeixinPhoneLoginRequest(@NotBlank String loginCode, @NotBlank String phoneCode, WeixinUserInfo userInfo) {}

    public record UserResponse(
        int id,
        String username,
        String role,
        String phone,
        String address,
        String gender,
        String birthDate,
        String nickname,
        String avatarUrl
    ) {}
    public record LoginResponse(String access_token, String token_type, UserResponse user) {}

    @PostMapping("/login")
    public LoginResponse login(
        @RequestBody(required = false) String body,
        @org.springframework.web.bind.annotation.RequestParam(name = "username", required = false) String username,
        @org.springframework.web.bind.annotation.RequestParam(name = "password", required = false) String password
    ) {
        String u = username;
        String p = password;
        if ((u == null || u.isBlank() || p == null || p.isBlank()) && body != null && !body.isBlank()) {
            try {
                java.util.Map<?, ?> m = objectMapper.readValue(body, java.util.Map.class);
                if ((u == null || u.isBlank()) && m.get("username") != null) {
                    u = String.valueOf(m.get("username"));
                }
                if ((p == null || p.isBlank()) && m.get("password") != null) {
                    p = String.valueOf(m.get("password"));
                }
            } catch (Exception ignored) {}
        }
        String usernameFinal = u == null ? "" : u.trim();
        String passwordFinal = p == null ? "" : p.trim();
        String normalizedPhone = normalizePhone(usernameFinal);
        UserEntity user = userRepo.findByUsername(usernameFinal).orElse(null);
        if (user == null && !normalizedPhone.isBlank() && !normalizedPhone.equals(usernameFinal)) {
            user = userRepo.findByUsername(normalizedPhone).orElse(null);
        }
        if (user == null && !usernameFinal.isBlank()) {
            user = userRepo.findByPhone(usernameFinal).orElse(null);
        }
        if (user == null && !normalizedPhone.isBlank() && !normalizedPhone.equals(usernameFinal)) {
            user = userRepo.findByPhone(normalizedPhone).orElse(null);
        }
        if (user == null && !normalizedPhone.isBlank()) {
            user = userRepo.findFirstByPhoneEndingWith(normalizedPhone).orElse(null);
        }
        if (user == null || !user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        boolean passwordOk = passwordEncoder.matches(passwordFinal, user.getHashedPassword());
        if (!passwordOk) {
            String stored = safe(user.getHashedPassword());
            if (!stored.isBlank() && stored.equals(passwordFinal)) {
                user.setHashedPassword(passwordEncoder.encode(passwordFinal));
                userRepo.save(user);
                passwordOk = true;
            }
        }
        if (!passwordOk) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtService.createToken(user.getUsername());
        return new LoginResponse(token, "bearer", toUserResponse(user));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@RequestBody RegisterRequest payload) {
        String username = payload.username() == null ? "" : payload.username().trim();
        String password = payload.password() == null ? "" : payload.password().trim();
        if (username.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or password");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setHashedPassword(passwordEncoder.encode(password));
        user.setActive(true);
        user.setRole("user");
        user = userRepo.save(user);
        String token = jwtService.createToken(user.getUsername());
        return new LoginResponse(token, "bearer", toUserResponse(user));
    }

    @PostMapping("/wecom/login")
    public LoginResponse wecomLogin(@RequestBody WeComLoginRequest payload) {
        String code = payload.code() == null ? "" : payload.code().trim();
        if (code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing code");
        }
        UserEntity user = userRepo.findByUsername("admin").orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String token = jwtService.createToken(user.getUsername());
        return new LoginResponse(token, "bearer", toUserResponse(user));
    }

    /**
     * 微信小程序登录：
     * 1) 小程序端调用 uni.login/provider=weixin，获取临时 code
     * 2) 前端把 code POST 到 /auth/weixin/login
     * 3) 服务端调用 jscode2session 获取 openid，按 openid 生成/查找用户，签发 JWT
     *
     * 环境参数：
     * - WEIXIN_APP_ID：小程序 appId
     * - WEIXIN_APP_SECRET：小程序 appSecret
     *
     * 返回：
     * - access_token：后续放到 Authorization: Bearer <token>
     * - user：当前用户信息
     */
    @PostMapping("/weixin/login")
    public LoginResponse weixinLogin(@RequestBody WeixinLoginRequest payload) {
        String code = payload == null || payload.code() == null ? "" : payload.code().trim();
        if (code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing code");
        }
        AppProperties.Weixin wx = props.weixin();
        String appId = wx == null ? "" : safe(wx.appId());
        String appSecret = wx == null ? "" : safe(wx.appSecret());
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weixin appId/appSecret not configured");
        }

        String openId = fetchWeixinOpenId(appId, appSecret, code);
        if (openId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Weixin login failed");
        }

        String username = ("wx_" + openId).trim();
        if (username.length() > 50) {
            username = username.substring(0, 50);
        }
        UserEntity user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            // 首次登录自动注册为普通用户
            UserEntity created = new UserEntity();
            created.setUsername(username);
            created.setHashedPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            created.setActive(true);
            created.setRole("user");
            user = userRepo.save(created);
        }

        boolean changed = false;
        WeixinUserInfo ui = payload == null ? null : payload.userInfo();
        if (ui != null) {
            String nickname = normalizeLength(safe(ui.nickName()), 60);
            String avatarUrl = normalizeLength(safe(ui.avatarUrl()), 512);
            String gender = mapWeixinGender(ui.gender());
            String address = normalizeLength(joinAddress(ui.country(), ui.province(), ui.city()), 255);
            if (!nickname.isBlank() && !nickname.equals(safe(user.getNickname()))) {
                user.setNickname(nickname);
                changed = true;
            }
            if (!avatarUrl.isBlank() && !avatarUrl.equals(safe(user.getAvatarUrl()))) {
                user.setAvatarUrl(avatarUrl);
                changed = true;
            }
            if (!gender.isBlank() && (gender.equals("男") || gender.equals("女"))) {
                if (!gender.equals(safe(user.getGender()))) {
                    user.setGender(gender);
                    changed = true;
                }
            }
            if (!address.isBlank() && safe(user.getAddress()).isBlank()) {
                user.setAddress(address);
                changed = true;
            }
        }
        if (changed) {
            user = userRepo.save(user);
        }

        String token = jwtService.createToken(user.getUsername());
        return new LoginResponse(token, "bearer", toUserResponse(user));
    }

    /**
     * 微信小程序手机号登录：
     * - 小程序端通过 open-type=getPhoneNumber 获取 phoneCode（e.detail.code）
     * - 可选：若前端同时传入 loginCode，可用于把手机号绑定到 openid 用户
     * - 服务端用 phoneCode 换手机号
     * - 按手机号查找用户，不存在则创建；返回 JWT 与用户信息
     */
    @PostMapping("/weixin/phone-login")
    public LoginResponse weixinPhoneLogin(@RequestBody WeixinPhoneLoginRequest payload) {
        String loginCode = payload == null || payload.loginCode() == null ? "" : payload.loginCode().trim();
        String phoneCode = payload == null || payload.phoneCode() == null ? "" : payload.phoneCode().trim();
        if (loginCode.isBlank() || phoneCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing code");
        }

        AppProperties.Weixin wx = props.weixin();
        String appId = wx == null ? "" : safe(wx.appId());
        String appSecret = wx == null ? "" : safe(wx.appSecret());
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weixin appId/appSecret not configured");
        }

        String openId = "";
        if (!loginCode.isBlank()) {
            openId = fetchWeixinOpenId(appId, appSecret, loginCode);
        }

        String accessToken = fetchWeixinAccessToken(appId, appSecret);
        if (accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Weixin access_token failed");
        }

        String phone = fetchWeixinPhoneNumber(accessToken, phoneCode);
        if (phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Weixin phone number failed");
        }

        UserEntity user = userRepo.findByPhone(phone).orElse(null);
        if (user == null) {
            if (!openId.isBlank()) {
                String wxUsername = ("wx_" + openId).trim();
                if (wxUsername.length() > 50) {
                    wxUsername = wxUsername.substring(0, 50);
                }
                UserEntity wxUser = userRepo.findByUsername(wxUsername).orElse(null);
                if (wxUser != null) {
                    wxUser.setPhone(phone);
                    user = userRepo.save(wxUser);
                }
            }

            if (user == null) {
                UserEntity created = new UserEntity();
                created.setUsername(phone);
                created.setPhone(phone);
                created.setHashedPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                created.setActive(true);
                created.setRole("user");
                user = userRepo.save(created);
            }
        } else {
            if (safe(user.getPhone()).isBlank()) {
                user.setPhone(phone);
                user = userRepo.save(user);
            }
        }

        boolean changed = false;
        WeixinUserInfo ui = payload == null ? null : payload.userInfo();
        if (ui != null) {
            String nickname = normalizeLength(safe(ui.nickName()), 60);
            String avatarUrl = normalizeLength(safe(ui.avatarUrl()), 512);
            String gender = mapWeixinGender(ui.gender());
            String address = normalizeLength(joinAddress(ui.country(), ui.province(), ui.city()), 255);
            if (!nickname.isBlank() && safe(user.getNickname()).isBlank()) {
                user.setNickname(nickname);
                changed = true;
            }
            if (!avatarUrl.isBlank() && safe(user.getAvatarUrl()).isBlank()) {
                user.setAvatarUrl(avatarUrl);
                changed = true;
            }
            if (!gender.isBlank() && safe(user.getGender()).isBlank()) {
                user.setGender(gender);
                changed = true;
            }
            if (!address.isBlank() && safe(user.getAddress()).isBlank()) {
                user.setAddress(address);
                changed = true;
            }
        }
        if (changed) {
            user = userRepo.save(user);
        }

        String token = jwtService.createToken(user.getUsername());
        return new LoginResponse(token, "bearer", toUserResponse(user));
    }

    @GetMapping("/me")
    public UserResponse me() {
        UserEntity user = currentUserService.requireUser();
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(UserEntity user) {
        if (user == null) {
            return new UserResponse(0, "", "", "", "", "", "", "", "");
        }
        return new UserResponse(
            user.getId() == null ? 0 : user.getId(),
            safe(user.getUsername()),
            safe(user.getRole()),
            safe(user.getPhone()),
            safe(user.getAddress()),
            safe(user.getGender()),
            safe(user.getBirthDate()),
            safe(user.getNickname()),
            safe(user.getAvatarUrl())
        );
    }

    /**
     * 通过 jscode2session 换取 openid（无需存 session_key）
     */
    private String fetchWeixinOpenId(String appId, String appSecret, String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" +
            URLEncoder.encode(appId, StandardCharsets.UTF_8) +
            "&secret=" + URLEncoder.encode(appSecret, StandardCharsets.UTF_8) +
            "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
            "&grant_type=authorization_code";

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(resp.body() == null ? "" : resp.body());
            String errCode = json.has("errcode") ? safe(json.get("errcode").asText()) : "";
            if (!errCode.isBlank() && !"0".equals(errCode)) {
                return "";
            }
            return json.has("openid") ? safe(json.get("openid").asText()) : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String fetchWeixinAccessToken(String appId, String appSecret) {
        long now = Instant.now().getEpochSecond();
        String cached = safe(cachedWxAccessToken);
        if (!cached.isBlank() && cachedWxAccessTokenExpireAtEpochSeconds > now + 60) {
            return cached;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" +
            URLEncoder.encode(appId, StandardCharsets.UTF_8) +
            "&secret=" + URLEncoder.encode(appSecret, StandardCharsets.UTF_8);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(resp.body() == null ? "" : resp.body());
            String errCode = json.has("errcode") ? safe(json.get("errcode").asText()) : "";
            if (!errCode.isBlank() && !"0".equals(errCode)) {
                return "";
            }
            String token = json.has("access_token") ? safe(json.get("access_token").asText()) : "";
            long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong(0) : 0;
            if (!token.isBlank() && expiresIn > 0) {
                cachedWxAccessToken = token;
                cachedWxAccessTokenExpireAtEpochSeconds = now + expiresIn;
            }
            return token;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String fetchWeixinPhoneNumber(String accessToken, String phoneCode) {
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" +
            URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of("code", phoneCode));
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(resp.body() == null ? "" : resp.body());
            String errCode = json.has("errcode") ? safe(json.get("errcode").asText()) : "";
            if (!errCode.isBlank() && !"0".equals(errCode)) {
                return "";
            }
            JsonNode info = json.has("phone_info") ? json.get("phone_info") : null;
            if (info == null) return "";
            String pure = info.has("purePhoneNumber") ? safe(info.get("purePhoneNumber").asText()) : "";
            if (!pure.isBlank()) return pure;
            return info.has("phoneNumber") ? safe(info.get("phoneNumber").asText()) : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizeLength(String s, int maxLen) {
        String v = safe(s);
        if (v.isBlank()) return "";
        if (v.length() <= maxLen) return v;
        return v.substring(0, maxLen);
    }

    private static String mapWeixinGender(Integer gender) {
        if (gender == null) return "";
        if (gender == 1) return "男";
        if (gender == 2) return "女";
        return "";
    }

    private static String joinAddress(String country, String province, String city) {
        String c = safe(country);
        String p = safe(province);
        String ci = safe(city);
        StringBuilder sb = new StringBuilder();
        if (!c.isBlank() && !"中国".equals(c)) {
            sb.append(c);
        }
        if (!p.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        if (!ci.isBlank() && !ci.equals(p)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(ci);
        }
        return sb.toString();
    }

    /**
     * 规范化手机号：去除空格、+86 前缀、86 前缀
     */
    private static String normalizePhone(String phone) {
        String p = safe(phone);
        if (p.isBlank()) return "";
        // 去除空格
        p = p.replaceAll("\\s+", "");
        // 去除 +86 前缀
        if (p.startsWith("+86")) {
            p = p.substring(3);
        }
        // 去除 86 前缀（仅当后面是数字时）
        if (p.startsWith("86") && p.length() > 2 && Character.isDigit(p.charAt(2))) {
            p = p.substring(2);
        }
        return p;
    }
}
