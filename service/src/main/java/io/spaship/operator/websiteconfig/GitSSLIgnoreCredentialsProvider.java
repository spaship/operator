package io.spaship.operator.websiteconfig;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * SSL Ignore {@link CredentialsProvider}
 */
public class GitSSLIgnoreCredentialsProvider extends CredentialsProvider {

    // Impl taken from https://github.com/eclipse/jgit/commit/d946f95c9c06f27de8aac6ecc3f5d49eabe6e030#diff-766ab2d3dbe64615288f45e71568fc60b369410bd23e86f4b35893dd459637e8R109

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... credentialItems) {
        for (CredentialItem item : credentialItems) {
            if (item instanceof CredentialItem.InformationalMessage) {
                continue;
            }
            if (item instanceof CredentialItem.YesNoType) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean get(URIish urIish, CredentialItem... credentialItems) throws UnsupportedCredentialItem {
        for (CredentialItem item : credentialItems) {
            if (item instanceof CredentialItem.InformationalMessage) {
                continue;
            }
            if (item instanceof CredentialItem.YesNoType) {
                ((CredentialItem.YesNoType) item).setValue(true);
                continue;
            }
            return false;
        }
        return true;
    }
}
