package fr.cnieg.keycloak;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthenticatorUserModel {

    private AuthenticatorUserModel() {
        throw new IllegalStateException("Utility class");
    }

    public static UserModel getUserModel(AuthenticationFlowContext context, String userName, String attributeKey2, String attributeRegex2) {
        AuthenticatorConfigModel authenticatorConfigModel = context.getAuthenticatorConfig();

        if (authenticatorConfigModel != null && authenticatorConfigModel.getConfig() != null && authenticatorConfigModel.getConfig().get(
                attributeKey2) != null && authenticatorConfigModel.getConfig().get(attributeRegex2) != null) {
            String attributeKey = authenticatorConfigModel.getConfig().get(attributeKey2);
            String attributeRegex = authenticatorConfigModel.getConfig().get(attributeRegex2);
            if (userName.matches(attributeRegex)) {
                
                /**
                 * if attributeKey does not contain "/", which means that attributeKey refers to org
                 * if so, then loginAlias will only refer to username
                 * it should get user by finduserbyusername
                 */
                String orgValue = userName.split("/")[0];
                String loginAliasValue = userName.split("/")[1];
                
                //loginAlias as username, try to find user by username directly
                UserModel userNameResult = context.getSession().users()
                        .getUserByUsername(context.getRealm(), loginAliasValue);
                if (userNameResult != null) {
                    //check whether the user has the orgValue or not
                    if (attributeKey.contains("/")) {
                        String org = attributeKey.split("/")[0];
                        
                        List<String> attributes = userNameResult.getAttributes().get(org);
                        if (attributes != null) {
                            if (attributes.size() > 0) {
                                if (attributes.contains(orgValue)) {
                                    return userNameResult;
                                }
                            }
                        }
                    }
                    List<String> attributes = userNameResult.getAttributes().get(attributeKey);
                    if (attributes != null) {
                        if (attributes.size() > 0) {
                            if (attributes.contains(orgValue)) {
                                return userNameResult;
                            }
                        }
                    }
                }
                
                if (attributeKey.contains("/")) {
                    String org = attributeKey.split("/")[0];
                    String loginAlias = attributeKey.split("/")[1];
                    
                    //user who uses loginAlias as username not exist, try to find user by loginAlias as attribute
                    List<UserModel> loginAliasResult = context.getSession().users()
                            .searchForUserByUserAttributeStream(context.getRealm(), loginAlias, loginAliasValue)
                            .collect(Collectors.toList());
                    if (loginAliasResult.size() == 1) {
                        return loginAliasResult.get(0);
                    }

                    // more than one user have same loginAlias, and the result should depend on org
                    if (loginAliasResult.size() > 1) {
                        List<UserModel> orgResult = context.getSession().users()
                                .searchForUserByUserAttributeStream(context.getRealm(), org, orgValue)
                                .filter(c -> loginAliasResult.contains(c))
                                .collect(Collectors.toList());
                        if (orgResult.size() == 1) {
                            return orgResult.get(0);
                        }
                    }
                }
            }
        }
        return null;
    }
}
