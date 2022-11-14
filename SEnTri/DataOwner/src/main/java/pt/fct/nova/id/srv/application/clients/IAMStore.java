package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.NewCookie;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessPolicyForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UserDTO;

import java.util.ArrayList;
import java.util.List;

public class IAMStore {

    public static boolean existsUser(String username, String password) {
        return false;
    }

    public static void saveUser(String username, String password) {

    }

    public static void updateUser(String username, UserDTO updatedUser) {

    }

    public static void deleteUser(String username) {

    }

    public static void saveIAMRequest(AccessPolicyForm iamRequest) {

    }

    public static void deleteIAMRequest(String requestID) {

    }

    public static List<String> getPendingIAMRequests(int total) {
        return new ArrayList<>();
    }

    public static NewCookie cacheSession(String username) {
        return null;
    }

    public static String getSession(String username) {
        return null;
    }
}
