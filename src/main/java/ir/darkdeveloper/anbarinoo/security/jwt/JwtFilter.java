package ir.darkdeveloper.anbarinoo.security.jwt;

import ir.darkdeveloper.anbarinoo.model.RefreshModel;
import ir.darkdeveloper.anbarinoo.service.RefreshService;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import ir.darkdeveloper.anbarinoo.util.UserUtils.UserAuthUtils;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
@AllArgsConstructor(onConstructor = @__(@Lazy))
public class JwtFilter extends OncePerRequestFilter {

    @Lazy
    private final JwtUtils jwtUtils;
    private final UserAuthUtils userAuthUtils;
    private final RefreshService refreshService;


    @Override
    protected void doFilterInternal(HttpServletRequest request,@NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        var refreshToken = request.getHeader("refresh_token");
        var accessToken = request.getHeader("access_token");

        if (refreshToken != null && accessToken != null && !jwtUtils.isTokenExpired(refreshToken)) {

            var username = jwtUtils.getUsername(refreshToken);
            var userId = ((Integer) jwtUtils.getAllClaimsFromToken(refreshToken).get("user_id")).longValue();

            authenticateUser(username);

            setUpHeader(response, accessToken, username, userId);
        }
        filterChain.doFilter(request, response);
    }


    private void authenticateUser(String username) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (username != null && auth == null) {
            //db query
            var userDetails = userAuthUtils.loadUserByUsername(username);
            var upToken = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(upToken);

        }
    }

    private void setUpHeader(HttpServletResponse response, String accessToken, String username,
                             Long userId) {

        String newAccessToken;

        // if this didn't execute, it means the access token is still valid
        if (jwtUtils.isTokenExpired(accessToken)) {
            //db query
            String storedAccessToken = refreshService.getRefreshByUserId(userId).getAccessToken();
            if (accessToken.equals(storedAccessToken)) {
                newAccessToken = jwtUtils.generateAccessToken(username);
                RefreshModel refreshModel = new RefreshModel();
                refreshModel.setAccessToken(newAccessToken);
                refreshModel.setUserId(userId);
                //db query
                refreshModel.setId(refreshService.getIdByUserId(userId));
                // db query
                refreshService.saveToken(refreshModel);
                response.addHeader("access_token", newAccessToken);
            } else
                //if stored token is not equal with user send token, it will return 403
                SecurityContextHolder.getContext().setAuthentication(null);

        }
    }

}
