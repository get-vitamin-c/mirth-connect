/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.server.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.io.RuntimeIOException;

import com.mirth.connect.client.core.Operation;
import com.mirth.connect.client.core.Operations;
import com.mirth.connect.model.LoginStatus;
import com.mirth.connect.model.LoginStatus.Status;
import com.mirth.connect.model.ServerEvent;
import com.mirth.connect.model.ServerEvent.Level;
import com.mirth.connect.model.ServerEvent.Outcome;
import com.mirth.connect.model.User;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerException;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.server.controllers.UserController;
import com.mirth.connect.server.util.UserSessionCache;

public class UserServlet extends MirthServlet {
    private Logger logger = Logger.getLogger(this.getClass());

    public static final String SESSION_USER = "user";
    public static final String SESSION_AUTHORIZED = "authorized";

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // MIRTH-1745
        response.setCharacterEncoding("UTF-8");

        UserController userController = ControllerFactory.getFactory().createUserController();
        EventController eventController = ControllerFactory.getFactory().createEventController();
        ConfigurationController configurationController = ControllerFactory.getFactory().createConfigurationController();
        PrintWriter out = response.getWriter();
        Operation operation = Operations.getOperation(request.getParameter("op"));
        ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

        try {
            if (operation.equals(Operations.USER_LOGIN)) {
                response.setContentType(TEXT_PLAIN);

                int tryCount = 0;
                int status = configurationController.getStatus();
                while (status != ConfigurationController.STATUS_INITIAL_DEPLOY && status != ConfigurationController.STATUS_OK) {
                    if (tryCount >= 5) {
                        serializer.serialize(new LoginStatus(Status.FAIL, "Server is still starting or otherwise unavailable. Please try again shortly."), out);
                        return;
                    }

                    Thread.sleep(1000);
                    status = configurationController.getStatus();
                    tryCount++;
                }

                String username = request.getParameter("username");
                String password = request.getParameter("password");
                String version = request.getParameter("version");
                serializer.serialize(login(request, response, userController, eventController, username, password, version), out);
            } else if (!isUserLoggedIn(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                Map<String, Object> parameterMap = new HashMap<String, Object>();

                if (operation.equals(Operations.USER_LOGOUT)) {
                    // Audit the logout request but don't block it
                    isUserAuthorized(request, null);

                    logout(request, userController);
                } else if (operation.equals(Operations.USER_GET)) {
                    /*
                     * If the requesting user does not have permission, only return themselves.
                     */
                    response.setContentType(APPLICATION_XML);
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    if (!isUserAuthorized(request, parameterMap)) {
                        user = new User();
                        user.setId(getCurrentUserId(request));
                    }

                    serializer.serialize(userController.getUser(user), out);
                } else if (operation.equals(Operations.USER_UPDATE)) {
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    if (isUserAuthorized(request, parameterMap) || isCurrentUser(request, user)) {
                        userController.updateUser(user);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else if (operation.equals(Operations.USER_CHECK_OR_UPDATE_PASSWORD)) {
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    if (isUserAuthorized(request, parameterMap) || isCurrentUser(request, user)) {
                        String plainPassword = request.getParameter("plainPassword");
                        serializer.serialize(userController.checkOrUpdateUserPassword(user.getId(), plainPassword), out);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else if (operation.equals(Operations.USER_REMOVE)) {
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    if (isUserAuthorized(request, parameterMap)) {
                        // Try to remove the user and then invalidate the
                        // session if it succeeded
                        userController.removeUser(user, (Integer) request.getSession().getAttribute(SESSION_USER));
                        UserSessionCache.getInstance().invalidateAllSessionsForUser(user);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else if (operation.equals(Operations.USER_IS_USER_LOGGED_IN)) {
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    if (isUserAuthorized(request, parameterMap)) {
                        response.setContentType(TEXT_PLAIN);
                        out.print(userController.isUserLoggedIn(user));
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else if (operation.equals(Operations.USER_PREFERENCES_GET)) {
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    // Allow if the user is requesting his own preferences.
                    // Check this first so a current user call is not audited.
                    if (isCurrentUser(request, user) || isUserAuthorized(request, parameterMap)) {
                        response.setContentType(TEXT_PLAIN);
                        serializer.serialize(userController.getUserPreferences(user), out);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else if (operation.equals(Operations.USER_PREFERENCES_SET)) {
                    User user = serializer.deserialize(request.getParameter("user"), User.class);
                    parameterMap.put("user", user);

                    // Allow if the user is setting his own preferences. Check
                    // this first so a current user call is not audited.
                    if (isCurrentUser(request, user) || isUserAuthorized(request, parameterMap)) {
                        String name = request.getParameter("name");
                        String value = request.getParameter("value");
                        userController.setUserPreference(user, name, value);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                }
            }
        } catch (RuntimeIOException rio) {
            logger.debug(rio);
        } catch (Throwable t) {
            logger.error(ExceptionUtils.getStackTrace(t));
            throw new ServletException(t);
        }
    }

    private LoginStatus login(HttpServletRequest request, HttpServletResponse response, UserController userController, EventController eventController, String username, String password, String version) throws ServletException {
        try {
            LoginStatus loginStatus = null;

            ConfigurationController configurationController = ControllerFactory.getFactory().createConfigurationController();

            // if the version of the client in is not the same as the server and
            // the version is not 0.0.0 (bypass)
            if (!version.equals(configurationController.getServerVersion()) && !version.equals("0.0.0")) {
                loginStatus = new LoginStatus(LoginStatus.Status.FAIL_VERSION_MISMATCH, "Mirth Connect Administrator version " + version + " cannot conect to Server version " + configurationController.getServerVersion() + ". Please clear your Java cache and relaunch the Administrator from the Server webpage.");
            } else {
                HttpSession session = request.getSession();

                loginStatus = userController.authorizeUser(username, password);

                if ((loginStatus.getStatus() == LoginStatus.Status.SUCCESS) || (loginStatus.getStatus() == LoginStatus.Status.SUCCESS_GRACE_PERIOD)) {
                    User user = new User();
                    user.setUsername(username);

                    List<User> validUsers = userController.getUser(user);

                    /*
                     * There must be a user to login with and store in the session, even if an
                     * Authorization Plugin returned a LoginStatus of SUCCESS
                     */
                    if (validUsers.size() < 1) {
                        loginStatus = new LoginStatus(LoginStatus.Status.FAIL, "Could not find a valid user with username: " + username);
                    } else {
                        User validUser = validUsers.get(0);

                        // set the sessions attributes
                        session.setAttribute(SESSION_USER, validUser.getId());
                        session.setAttribute(SESSION_AUTHORIZED, true);

                        // this prevents the session from timing out
                        session.setMaxInactiveInterval(-1);

                        // set the user status to logged in in the database
                        userController.loginUser(validUser);

                        // add the user's session to to session map
                        UserSessionCache.getInstance().registerSessionForUser(session, validUser);
                    }
                }
            }

            // Manually audit the Login event with the username since the user
            // id has not been stored to the session yet
            ServerEvent event = new ServerEvent();
            event.setIpAddress(getRequestIpAddress(request));
            event.setLevel(Level.INFORMATION);
            event.setName(Operations.USER_LOGIN.getDisplayName());

            // Set the outcome to the result of the login attempt
            event.setOutcome(((loginStatus.getStatus() == LoginStatus.Status.SUCCESS) || (loginStatus.getStatus() == LoginStatus.Status.SUCCESS_GRACE_PERIOD)) ? Outcome.SUCCESS : Outcome.FAILURE);

            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put("username", username);
            event.setAttributes(attributes);

            eventController.dispatchEvent(event);

            return loginStatus;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void logout(HttpServletRequest request, UserController userController) throws ServletException {
        HttpSession session = request.getSession();

        // save the session id before removing them from the session
        Integer userId = (Integer) session.getAttribute(SESSION_USER);
        String sessionId = session.getId();

        // remove the sessions attributes
        session.removeAttribute(SESSION_USER);
        session.removeAttribute(SESSION_AUTHORIZED);

        // invalidate the current sessions
        session.invalidate();

        // set the user status to logged out in the database
        User user = new User();
        user.setId(userId);

        try {
            userController.logoutUser(user);
        } catch (ControllerException ce) {
            throw new ServletException(ce);
        }
    }

    private boolean isCurrentUser(HttpServletRequest request, User user) {
        return user.getId() == getCurrentUserId(request);
    }
}
