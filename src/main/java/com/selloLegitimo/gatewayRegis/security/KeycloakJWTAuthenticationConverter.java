package com.selloLegitimo.gatewayRegis.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class KeycloakJWTAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakJWTAuthenticationConverter.class);

	private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
		String principalName = jwt.getClaimAsString("preferred_username");

		if (principalName == null || principalName.isBlank()) {
			principalName = jwt.getSubject();
		}

		LOGGER.info(
				"JWT validated subject={} principal={} issuer={} authorities={}",
				jwt.getSubject(),
				principalName,
				jwt.getIssuer(),
				authorities.stream().map(GrantedAuthority::getAuthority).toList());

		return new JwtAuthenticationToken(jwt, authorities, principalName);
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>(defaultAuthoritiesConverter.convert(jwt));
		authorities.addAll(extractRealmRoles(jwt));
		authorities.addAll(extractResourceRoles(jwt));
		return authorities;
	}

	private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null) {
			return List.of();
		}

		return mapRoles(realmAccess.get("roles"));
	}

	private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
		Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess == null || resourceAccess.isEmpty()) {
			return List.of();
		}

		List<GrantedAuthority> authorities = new ArrayList<>();
		for (Object resource : resourceAccess.values()) {
			if (resource instanceof Map<?, ?> resourceMap) {
				authorities.addAll(mapRoles(resourceMap.get("roles")));
			}
		}

		return authorities;
	}

	private Collection<GrantedAuthority> mapRoles(Object rolesClaim) {
		if (!(rolesClaim instanceof Collection<?> roles)) {
			return List.of();
		}

		return roles.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.filter(role -> !role.isBlank())
				.map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase().replace('-', '_'))
				.map(SimpleGrantedAuthority::new)
				.map(GrantedAuthority.class::cast)
				.toList();
	}
}