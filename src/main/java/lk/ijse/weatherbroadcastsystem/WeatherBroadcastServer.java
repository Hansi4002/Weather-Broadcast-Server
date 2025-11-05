package lk.ijse.weatherbroadcastsystem;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class WeatherBroadcastServer {
    private static final int PORT = 4000;
    private static final List<DataOutputStream> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("Starting Weather Broadcast Server on port " + PORT);
        ExecutorService executor = Executors.newCachedThreadPool();

        // thread: accept clients
        executor.execute(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("Client connected: " + client.getRemoteSocketAddress());
                    DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                    clients.add(dos);

                    // optional per-client reader to detect disconnection or client messages
                    executor.execute(() -> handleClient(client));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // periodic weather generator: every 5 seconds (adjust as needed)
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            String update = generateWeatherUpdate();
            broadcast(update);
            System.out.println("Broadcasted: " + update);
        }, 0, 5, TimeUnit.SECONDS);
    }

    private static void handleClient(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            while (true) {
                // If clients need to send something in future (e.g., subscribe), handle here
                String msg = dis.readUTF();
                System.out.println("Received from client: " + msg);
                // For now ignore or respond
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
            removeClientSocket(socket);
        }
    }

    private static void removeClientSocket(Socket socket) {
        try {
            DataOutputStream toRemove = new DataOutputStream(socket.getOutputStream());
            synchronized (clients) {
                clients.removeIf(dos -> {
                    try {
                        // best-effort compare by checking equality of underlying stream (not guaranteed)
                        return dos == toRemove;
                    } catch (Exception ex) {
                        return false;
                    }
                });
            }
        } catch (IOException ignored) {}
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            Iterator<DataOutputStream> iterator = clients.iterator();
            while (iterator.hasNext()) {
                DataOutputStream dos = iterator.next();
                try {
                    dos.writeUTF(message);
                    dos.flush();
                } catch (IOException e) {
                    // remove dead clients
                    iterator.remove();
                }
            }
        }
    }

    // Simple simulated weather generator
    private static final Random rand = new Random();
    private static final String[] CONDITIONS = {"Sunny", "Cloudy", "Rain", "Storm", "Windy", "Foggy", "Snow"};

    private static String generateWeatherUpdate() {
        String city = pickCity(); // random city or global broadcast
        int temp = rand.nextInt(35) - 5 + rand.nextInt(6); // approx -5 .. 40
        int humidity = 20 + rand.nextInt(80);
        String cond = CONDITIONS[rand.nextInt(CONDITIONS.length)];
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // simple structured message (client can parse if needed)
        return String.format("%s | %s | Temp: %d°C | Humidity: %d%% | %s", time, city, temp, humidity, cond);
    }

    private static String pickCity() {
        String[] cities = {"Colombo","Kandy","Galle","Jaffna","Trincomalee","Matara"};
        return cities[rand.nextInt(cities.length)];
    }
}
