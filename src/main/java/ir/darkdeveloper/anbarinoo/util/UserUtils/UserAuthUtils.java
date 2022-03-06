package ir.darkdeveloper.anbarinoo.util.UserUtils;

import ir.darkdeveloper.anbarinoo.exception.BadRequestException;
import ir.darkdeveloper.anbarinoo.exception.EmailNotValidException;
import ir.darkdeveloper.anbarinoo.exception.ForbiddenException;
import ir.darkdeveloper.anbarinoo.exception.NoContentException;
import ir.darkdeveloper.anbarinoo.model.Auth.AuthProvider;
import ir.darkdeveloper.anbarinoo.model.RefreshModel;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.repository.UserRepo;
import ir.darkdeveloper.anbarinoo.dto.LoginDto;
import ir.darkdeveloper.anbarinoo.service.RefreshService;
import ir.darkdeveloper.anbarinoo.service.UserRolesService;
import ir.darkdeveloper.anbarinoo.util.AdminUserProperties;
import ir.darkdeveloper.anbarinoo.util.IOUtils;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAuthUtils {

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final RefreshService refreshService;
    private final UserRepo repo;
    private final PasswordEncoder encoder;
    private final AdminUserProperties adminUser;
    private final IOUtils ioUtils;
    private final UserRolesService roleService;
    private final Boolean userEnabled;
    private final Operations operations;
    private final PasswordUtils passwordUtils;

    public static final DateTimeFormatter TOKEN_EXPIRATION_FORMAT =
            DateTimeFormatter.ofPattern("EE MMM dd yyyy HH:mm:ss");


    @Transactional
    public void signup(UserModel user, HttpServletResponse response) {
        if (user.getId() != null)
            throw new ForbiddenException("You are not allowed to sign up! :|");

        var rawPass = user.getPassword();

        if (user.getEmail() != null)
            if (user.getUserName() == null || user.getUserName().trim().equals(""))
                user.setUserName(user.getEmail().split("@")[0]);

        passwordUtils.passEqualityChecker(user);

        user.setRoles(roleService.findAllByName("USER"));
        ioUtils.saveUserImages(user);
        user.setPassword(encoder.encode(user.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setEnabled(userEnabled);
        repo.save(user);
        if (!user.getEnabled())
            operations.sendEmail(user);
        else
            authenticateUser(new LoginDto(user.getEmail(), rawPass), rawPass, response);

    }

    /**
     * @param loginDto has username and password (LoginDto)
     * @param rawPass  for super admin, pass null
     */
    public void authenticateUser(LoginDto loginDto, String rawPass, HttpServletResponse response) {
        var username = loginDto.username();
        var password = loginDto.password();

        var user = repo.findByEmailOrUsername(username)
                .orElseThrow(() -> new NoContentException("User does not exist"));

        UsernamePasswordAuthenticationToken auth;
        if (rawPass != null)
            auth = new UsernamePasswordAuthenticationToken(username, rawPass);
        else
            auth = new UsernamePasswordAuthenticationToken(username, password);

        try {
            authManager.authenticate(auth);
        } catch (DisabledException e) {
            throw new BadRequestException("Email is not verified!");
        } catch (Exception e) {
            throw new BadRequestException("Bad Credentials");
        }

        if (!user.getEnabled()) {
            operations.sendEmail(user);
            throw new EmailNotValidException("Email is not verified! Check your emails");
        }

        var rModel = new RefreshModel();
        if (loginDto.username().equals(adminUser.getUsername())) {
            rModel.setUserId(adminUser.getId());
            rModel.setId(refreshService.getIdByUserId(adminUser.getId()));
        } else {
            rModel.setId(refreshService.getIdByUserId(user.getId()));
            rModel.setUserId(user.getId());
        }

        var accessToken = jwtUtils.generateAccessToken(username);
        var refreshToken = jwtUtils.generateRefreshToken(username, rModel.getUserId());

        rModel.setAccessToken(accessToken);

        refreshService.saveToken(rModel);

        setupHeader(response, accessToken, refreshToken);
    }

    public void setupHeader(HttpServletResponse response, String accessToken, String refreshToken) {
        var date = jwtUtils.getExpirationDate(refreshToken);
        var refreshDate = TOKEN_EXPIRATION_FORMAT.format(date);
//        var accessDate = TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(accessToken));
        response.addHeader("refresh_token", refreshToken);
        response.addHeader("access_token", accessToken);
        response.addHeader("refresh_expiration", refreshDate);
//        response.addHeader("access_expiration", accessDate);
    }


    public Optional<? extends UserDetails> loadUserByUsername(String username) {
        if (username.equals(adminUser.getUsername())) {
            var authorities = adminUser.getAuthorities();
            return Optional.of(
                    User.builder().username(adminUser.getUsername())
                            .password(encoder.encode(adminUser.getPassword())).authorities(authorities).build()
            );
        }
        return repo.findByEmailOrUsername(username);
    }

}
