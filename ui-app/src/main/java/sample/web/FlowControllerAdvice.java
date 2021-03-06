/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Joe Grandja
 */
@ControllerAdvice
public class FlowControllerAdvice {

	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	@ModelAttribute("idTokenClaims")
	Map<String, Object> idTokenClaims(@AuthenticationPrincipal OidcUser oidcUser) {
		if (oidcUser == null) {
			return Collections.emptyMap();
		}
		final List<String> claimNames = Arrays.asList("iss", "sub", "aud", "azp", "given_name", "family_name", "email");
		return oidcUser.getClaims().entrySet().stream()
				.filter(e -> claimNames.contains(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@ModelAttribute("authorizedClientRegistrations")
	List<AuthorizedClientRegistrationModel> authorizedClientRegistrations(OAuth2AuthenticationToken oauth2Authentication) {
		List<AuthorizedClientRegistrationModel> authorizedClientRegistrations = new ArrayList<>();
		getClientRegistrations().forEach(registration -> {
			OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient(
					registration.getRegistrationId(), oauth2Authentication.getName());
			authorizedClientRegistrations.add(
					new AuthorizedClientRegistrationModel(registration, authorizedClient));

		});
		authorizedClientRegistrations.sort(Comparator.comparing(AuthorizedClientRegistrationModel::getClientId));
		return authorizedClientRegistrations;
	}

	private List<ClientRegistration> getClientRegistrations() {
		ResolvableType type = ResolvableType.forInstance(this.clientRegistrationRepository).as(Iterable.class);
		if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
			return StreamSupport.stream(((Iterable<ClientRegistration>) clientRegistrationRepository).spliterator(), false)
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}