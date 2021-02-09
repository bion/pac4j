package org.pac4j.saml.redirect;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.core.redirect.RedirectionActionBuilder;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.profile.api.SAML2ObjectBuilder;
import org.pac4j.saml.sso.impl.SAML2AuthnRequestBuilder;

import java.util.Optional;

/**
 * Redirection action builder for SAML 2.
 *
 * @author Jerome Leleu
 * @since 2.0.0
 */
public class SAML2RedirectionActionBuilder implements RedirectionActionBuilder {

    protected SAML2ObjectBuilder<AuthnRequest> saml2ObjectBuilder;

    private final SAML2Client client;

    public SAML2RedirectionActionBuilder(final SAML2Client client) {
        CommonHelper.assertNotNull("client", client);
        this.client = client;
        final var cfg = client.getConfiguration();
        this.saml2ObjectBuilder = new SAML2AuthnRequestBuilder(cfg);
    }

    @Override
    public Optional<RedirectionAction> getRedirectionAction(final WebContext wc, final SessionStore sessionStore) {
        final var context = this.client.getContextProvider().buildContext(wc, sessionStore);
        final var relayState = this.client.getStateGenerator().generateValue(wc, sessionStore);

        final var authnRequest = this.saml2ObjectBuilder.build(context);
        this.client.getProfileHandler().send(context, authnRequest, relayState);

        final var adapter = context.getProfileRequestContextOutboundMessageTransportResponse();

        final var bindingType = this.client.getConfiguration().getAuthnRequestBindingType();
        if (SAMLConstants.SAML2_POST_BINDING_URI.equalsIgnoreCase(bindingType) ||
            SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI.equalsIgnoreCase(bindingType)) {
            final var content = adapter.getOutgoingContent();
            return Optional.of(HttpActionHelper.buildFormPostContentAction(wc, content));
        }
        final var location = adapter.getRedirectUrl();
        return Optional.of(HttpActionHelper.buildRedirectUrlAction(wc, location));
    }
}
