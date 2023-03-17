package fr.cnieg.keycloak;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;

public class AuthenticatorUserModel {

    private AuthenticatorUserModel() {
        throw new IllegalStateException("Utility class");
    }

    public static UserModel getUserModel(AuthenticationFlowContext context, String userName, String attributeKey2, String attributeRegex2) {
        AuthenticatorConfigModel authenticatorConfigModel = context.getAuthenticatorConfig();

        if (authenticatorConfigModel != null && authenticatorConfigModel.getConfig() != null && authenticatorConfigModel.getConfig().get(
                attributeKey2) != null && authenticatorConfigModel.getConfig().get(attributeRegex2) != null) {
            ///support multi attributes used for login
            ///solution 1
            ///support user to set attribute as String with format like `attribute1;attribute2`
            String attributeKey = authenticatorConfigModel.getConfig().get(attributeKey2);
            String attributeRegex = authenticatorConfigModel.getConfig().get(attributeRegex2);
            if (userName.matches(attributeRegex)) {
                String[] attributeKeyList = attributeKey.split("##");
                for (int i = 0; i < attributeKeyList.length; i++) {
                    List<UserModel> result = context.getSession().users()
                        .searchForUserByUserAttributeStream(context.getRealm(), attributeKeyList[i], userName)
                        .collect(Collectors.toList());
                    if (result.size() == 1) {
                        return result.get(0);
                    }
                }
            }
            ///solution 2
            ///set the setting of the attribute type with `ProviderConfigProperty.MULTIVALUED_STRING_TYPE`
            ///TODO how to get the attribute as list
            // List<String> attributeKeysString = authenticatorConfigModel.getConfig().get(attributeKey2);
            // System.console().printf(attributeKeysString);
            // List<String> attributeKeys = attributeKeysString.toList();
            // String attributeRegex = authenticatorConfigModel.getConfig().get(attributeRegex2);
            // if (userName.matches(attributeRegex)) {
            //     for (String attributeKey : attributeKeys) {
            //         List<UserModel> result = context.getSession().users()
            //             .searchForUserByUserAttributeStream(context.getRealm(), attributeKey, userName)
            //             .collect(Collectors.toList());
            //         if (result.size() == 1) {
            //             return result.get(0);
            //         }
            //     }
            // }
        }
        return null;
    }
}
