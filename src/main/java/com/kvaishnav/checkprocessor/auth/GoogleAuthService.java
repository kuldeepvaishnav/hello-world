package com.kvaishnav.checkprocessor.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.GmailScopes;
import com.kvaishnav.checkprocessor.config.AppConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Handles Google OAuth 2.0 authentication for the installed-app flow.
 *
 * <p>On the first run it opens a browser window prompting the user to log in
 * and grant permissions. The resulting token is stored in the configured
 * tokens directory and reused (with automatic refresh) on subsequent runs.
 *
 * <p>Scopes requested:
 * <ul>
 *   <li>{@code drive} — read input folder, create output folder, upload files</li>
 *   <li>{@code gmail.send} — send notification emails via Gmail API</li>
 * </ul>
 */
public class GoogleAuthService {

    private static final List<String> SCOPES = List.of(
            DriveScopes.DRIVE,
            GmailScopes.GMAIL_SEND
    );

    private final AppConfig config;
    private final NetHttpTransport transport;
    private final GsonFactory jsonFactory;

    public GoogleAuthService(AppConfig config, NetHttpTransport transport, GsonFactory jsonFactory) {
        this.config = config;
        this.transport = transport;
        this.jsonFactory = jsonFactory;
    }

    /**
     * Performs the OAuth flow if no valid token exists, otherwise loads the
     * cached token and returns a ready-to-use {@link Credential}.
     */
    public Credential authorize() throws Exception {
        GoogleClientSecrets secrets;
        try (var in = new FileInputStream(config.getCredentialsPath())) {
            secrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(config.getTokensDirectory())))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
