package com.brijesh.authservice.security.oauth2;

import com.brijesh.authservice.domain.enums.AuthProvider;
import com.brijesh.authservice.domain.exception.AuthException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class OAuth2UserInfoFactory  {

    private OAuth2UserInfoFactory(){}

    public static OAuth2UserInfo extract(String registrationId, OAuth2User oAuth2User){
        Map<String,Object> attributes = oAuth2User.getAttributes();

        return switch (registrationId.toLowerCase()){
            case "google" -> extractGoogle(attributes);
            case "github" -> extractGithub(attributes);
            default -> throw new AuthException("Unsupported OAuth2 provider: "+registrationId);
        };
    }


    private static OAuth2UserInfo extractGoogle(Map<String,Object> attrs){
        String fullName = (String) attrs.getOrDefault("name","");
        String[] parts = fullName.split(" ", 2);

        return new OAuth2UserInfo(
                (String) attrs.get("sub"),             // Google's user ID
                (String) attrs.get("email"),
                parts.length > 0 ? parts[0] : "",
                parts.length > 1 ? parts[1] : "",
                AuthProvider.GOOGLE
        );
    }

    private static OAuth2UserInfo extractGithub(Map<String, Object> attrs) {
        String fullName = (String) attrs.getOrDefault("name", "");
        String[] parts = fullName.split(" ",2);
        String email = (String) attrs.get("email");

        // Github email can be null if user set it private
        if (email == null) {
            email = attrs.get("login") + "@github.com";  // fallback
        }

        return new OAuth2UserInfo(
                String.valueOf(attrs.get("id")),              // Github's user ID
                email,
                parts.length > 0 ? parts[0] : "",
                parts.length > 1 ? parts[1] : "",
                AuthProvider.GITHUB
        );
    }

}
