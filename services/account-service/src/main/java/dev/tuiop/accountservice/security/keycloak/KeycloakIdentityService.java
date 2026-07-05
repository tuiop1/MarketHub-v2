package dev.tuiop.accountservice.security.keycloak;


import dev.tuiop.accountservice.security.keycloak.config.KeycloakAdminProperties;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakIdentityService {


    private final Keycloak keycloak;

    private final String realmName;


    public KeycloakIdentityService(
            Keycloak keycloak,
            KeycloakAdminProperties properties
    ){
        this.keycloak = keycloak;
        this.realmName = properties.realm();
    }


    public String createUser(
            String email,
            String password,
            RealmRole realmRole

    ){
        return createUser(email, password, null, null, realmRole);
    }

    public String createUser(
            String email,
            String password,
            String firstName,
            String lastName,
            RealmRole realmRole

    ){
        RealmResource realm = keycloak.realm(realmName);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setCredentials(List.of(passwordCredential(password)));


        String userId;

        try(Response response = realm.users().create(user)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                    throw new EmailAlreadyTakenException(email);
                }

                throw new IllegalStateException(
                        "Keycloak user creation failed with status " + response.getStatus()
                );
            }


            userId = CreatedResponseUtil.getCreatedId(response);



        }
        try {
            RoleRepresentation role = realm.roles()
                    .get(realmRole.toString())
                    .toRepresentation();

            realm.users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));

            return userId;
        } catch (RuntimeException exception) {
            realm.users().delete(userId);
            throw exception;
        }
    }


    public void deleteUser(String userId) {
        keycloak.realm(realmName)
                .users()
                .delete(userId);
    }

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential =
                new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        return credential;
    }


    public void addRealmRole(String userId, RealmRole realmRole) {

        String roleName = realmRole.name();
        RealmResource realm = keycloak.realm(realmName);

        RoleRepresentation role = realm.roles()
                .get(roleName)
                .toRepresentation();

        realm.users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }

    public void removeRealmRole(String userId, RealmRole realmRole) {
        String roleName = realmRole.name();
        RealmResource realm = keycloak.realm(realmName);

        RoleRepresentation role = realm.roles()
                .get(roleName)
                .toRepresentation();

        realm.users()
                .get(userId)
                .roles()
                .realmLevel()
                .remove(List.of(role));
    }

    public void enableUser(String keycloakUserId) {
        UserResource userResource = keycloak.realm(realmName)
                .users()
                .get(keycloakUserId);

        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(true);
        userResource.update(user);
    }


    public void disableUser(String keycloakUserId) {
        UserResource userResource = keycloak.realm(realmName)
                .users()
                .get(keycloakUserId);

        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(false);
        userResource.update(user);

        userResource.logout();
    }

}
