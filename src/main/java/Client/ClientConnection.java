package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientConnection implements Runnable {
    private static Socket socket;
    private static DataOutputStream out;

    private static String nick;
    private static String login;
    private static String pass;
    private Boolean register;

    private final ChatController chatController;

    public ClientConnection(String login, String pass, ChatController chatController) {
        ClientConnection.login = login;
        ClientConnection.pass  = pass;

        this.chatController = chatController;
        this.register       = false;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    private void register() {
        sendMessage("/register " + ClientConnection.login + " " + ClientConnection.pass);
    }

    private void login() {
        sendMessage("/auth " + ClientConnection.login + " " + ClientConnection.pass);
    }

    public static void logout() {
        nick = null;

        sendMessage("/end");
    }

    public static void delete() {
        nick = null;

        sendMessage("/delete");
    }

    public static void sendMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            socket = new Socket("localhost", 8189);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            if (this.register) {
                this.register();
            } else {
                this.login();
            }


            while (socket.isConnected()) {
                String msg = in.readUTF();

                if (nick != null) {
                    if (msg.startsWith("/")) {
                        if (msg.equalsIgnoreCase("/end")) {
                            this.chatController.showLogin();
                        } else if (msg.equalsIgnoreCase("/delete")) {
                            this.chatController.showLogin();
                        } else if (msg.startsWith("/user_list ")) {
                            String[] users = msg.split(" ");

                            this.chatController.updateUserList(Arrays.copyOfRange(users, 1, users.length));
                        }
                    } else {
                        if (!msg.isEmpty()) {
                            this.chatController.addMessage(msg);
                        }
                    }
                } else {
                    if (msg.startsWith("/auth_ok")) {
                        String[] elements = msg.split(" ");
                        nick = elements[1];
                        this.chatController.setNickLabel(nick);

                        LoginController.getInstance().showScene();
                    } else if (msg.startsWith("/auth_fail ")) {
                        String response = msg.substring(11);
                        LoginController.getInstance().showResponse(response);

                        break;
                    } else if (msg.startsWith("/register_ok ")) {
                        String[] elements = msg.split(" ");
                        nick = elements[1];
                        this.chatController.setNickLabel(nick);

                        LoginController.getInstance().showScene();
                    } else if (msg.startsWith("/register_fail ")) {
                        String response = msg.substring(15);
                        LoginController.getInstance().showResponse(response);

                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}