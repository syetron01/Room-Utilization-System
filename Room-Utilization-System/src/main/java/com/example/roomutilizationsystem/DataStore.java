package com.example.roomutilizationsystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// --- Enums (Can be separate files, but included here for simplicity) ---
enum UserRole {
    ADMIN, FACULTY // STAFF is represented by FACULTY here
}

enum BookingStatus {
    PENDING, APPROVED, REJECTED, CANCELLED
}

// --- Data Classes ---
class User {
    private String username;
    private String password; // WARNING: Plain text password! Hash in production.
    private UserRole role;

    public User(String username, String password, UserRole role) {
        this.username = username;
        this.password = password; // Store hashed password in production
        this.role = role;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; } // Should return hash in production
    public UserRole getRole() { return role; }

    // Need equals and hashCode if used in comparisons or collections based on object identity
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}

// Represents the availability template defined by the Admin
class RoomSchedule {
    private final String scheduleId; // Unique ID for the schedule entry
    private String roomNumber;
    private String roomType; // e.g., "Laboratory", "Lecture"
    private LocalTime startTime;
    private LocalTime endTime;
    private DayOfWeek dayOfWeek;

    // Static counter for unique IDs (Used for NEW schedules)
    // FIX: Changed from private to package-private (removed "private")
    static final AtomicLong idCounter = new AtomicLong(0); // Start from 0

    // Constructor for creating NEW schedules (ID is generated)
    public RoomSchedule(String roomNumber, String roomType, LocalTime startTime, LocalTime endTime, DayOfWeek dayOfWeek) {
        this.scheduleId = "SCH-" + idCounter.incrementAndGet(); // Generate and assign ID
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
    }

    // !! NEW Constructor for LOADING existing schedules (ID is provided)
    public RoomSchedule(String scheduleId, String roomNumber, String roomType, LocalTime startTime, LocalTime endTime, DayOfWeek dayOfWeek) {
        this.scheduleId = scheduleId; // Use the provided ID
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
        // Update the static counter if the loaded ID is higher
        try {
            // Extract the numeric part of the ID
            String idNumStr = scheduleId.replace("SCH-", "");
            if (!idNumStr.isEmpty()) {
                long idNum = Long.parseLong(idNumStr);
                // Atomically update counter if needed
                // FIX: Accessing idCounter is now allowed (package-private)
                idCounter.updateAndGet(current -> Math.max(current, idNum + 1));
            }
        } catch (NumberFormatException e) {
            System.err.println("Could not parse schedule ID: " + scheduleId + " for counter update.");
        }
    }

    // Getters (Keep as is)
    public String getScheduleId() { return scheduleId; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }

    // Formatter for display (Keep as is)
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    // For display in tables or lists (Keep as is)
    public String getTimeRangeString() {
        return startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
    }

    // Derived property for TableView (matches Time Column in FXML) (Keep as is)
    public String getTimeColDisplay() { return getTimeRangeString(); }

    // Derived property for TableView (matches Day Column in FXML) (Keep as is)
    public String getDayColDisplay() {
        String dayStr = dayOfWeek.toString();
        return dayStr.substring(0, 1).toUpperCase() + dayStr.substring(1).toLowerCase();
    }

    @Override
    public String toString() { // For debugging or simple lists (Keep as is)
        return "Schedule{" +
                "id='" + scheduleId + '\'' +
                ", room='" + roomNumber + '\'' +
                ", type='" + roomType + '\'' +
                ", day=" + dayOfWeek +
                ", time=" + getTimeRangeString() +
                '}';
    }

    // Need equals/hashCode if comparing schedules directly (Keep as is)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomSchedule that = (RoomSchedule) o;
        return Objects.equals(scheduleId, that.scheduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId);
    }
}

// Represents an actual booking made by a Faculty member
class Booking {
    private final String bookingId;
    // Store username directly, look up User object when needed
    private String facultyUsername;
    // Store room details directly (from the schedule *at the time of booking*)
    private String roomNumber;
    private String roomType;

    private LocalDate date;
    private LocalTime bookedStartTime;
    private LocalTime bookedEndTime;
    private BookingStatus status;

    // Static counter for unique IDs (Used for NEW bookings)
    // FIX: Changed from private to package-private (removed "private")
    static final AtomicLong idCounter = new AtomicLong(0); // Start from 0

    // Constructor for creating NEW bookings (ID is generated)
    public Booking(User facultyUser, RoomSchedule roomSchedule, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime) {
        this.bookingId = "BOK-" + idCounter.incrementAndGet(); // Generate and assign ID
        if (facultyUser == null || facultyUser.getRole() != UserRole.FACULTY) {
            throw new IllegalArgumentException("Invalid user for booking. Must be Faculty.");
        }
        if (roomSchedule == null || date == null || bookedStartTime == null || bookedEndTime == null) {
            throw new IllegalArgumentException("Booking details cannot be null.");
        }
        // Basic time validation (more detailed duration checks happen in DataStore.addBooking)
        if (!bookedEndTime.isAfter(bookedStartTime)) { // Use isAfter for strict check
            throw new IllegalArgumentException("Booking end time must be after start time.");
        }

        this.facultyUsername = facultyUser.getUsername(); // Store username
        this.roomNumber = roomSchedule.getRoomNumber(); // Store room details from schedule
        this.roomType = roomSchedule.getRoomType();   // Store room details from schedule
        this.date = date;
        this.bookedStartTime = bookedStartTime;
        this.bookedEndTime = bookedEndTime;
        this.status = BookingStatus.PENDING; // Default status
    }

    // !! NEW Constructor for LOADING existing bookings (ID and details are provided)
    public Booking(String bookingId, String facultyUsername, String roomNumber, String roomType, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime, BookingStatus status) {
        this.bookingId = bookingId; // Use the provided ID
        this.facultyUsername = facultyUsername;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.date = date;
        this.bookedStartTime = bookedStartTime;
        this.bookedEndTime = bookedEndTime;
        this.status = status;
        // Update the static counter if the loaded ID is higher
        try {
            // Extract the numeric part of the ID
            String idNumStr = bookingId.replace("BOK-", "");
            if (!idNumStr.isEmpty()) {
                long idNum = Long.parseLong(idNumStr);
                // Atomically update counter if needed
                // FIX: Accessing idCounter is now allowed (package-private)
                idCounter.updateAndGet(current -> Math.max(current, idNum + 1));
            }
        } catch (NumberFormatException e) {
            System.err.println("Could not parse booking ID: " + bookingId + " for counter update.");
        }
    }


    // Getters
    public String getBookingId() { return bookingId; }
    // Look up User object from DataStore when needed (can return null if user was deleted)
    public User getFacultyUser() { return DataStore.getUserByUsername(facultyUsername); }
    public String getFacultyUsername() { return facultyUsername; } // Direct getter for username

    // Getters for stored room details
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }

    public LocalDate getDate() { return date; }
    public LocalTime getBookedStartTime() { return bookedStartTime; }
    public LocalTime getBookedEndTime() { return bookedEndTime; }
    public BookingStatus getStatus() { return status; }

    // Setter for status (used by Admin or for cancellation)
    public void setStatus(BookingStatus status) {
        this.status = Objects.requireNonNull(status, "Booking status cannot be null");
        // Automatically save data when status changes
        DataStore.saveData();
    }

    // --- Derived properties for TableView Columns ---
    // Make sure these method names match the PropertyValueFactory strings in controllers

    // For AdminManageBookingsController - roleCol
    public String getFacultyRole() {
        User user = getFacultyUser();
        return user != null ? user.getRole().toString() : "UNKNOWN"; // Handle case if user lookup fails
    }

    // For time display in tables - timeCol, penTimeCol, appTimeCol (Use booked times)
    public String getTimeScheduleDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return bookedStartTime.format(formatter) + " - " + bookedEndTime.format(formatter);
    }
    // Alias for consistency if needed
    public String getBookedTimeRangeString() { return getTimeScheduleDisplay(); }


    // For day display in tables - dayCol, penDayCol, appDayCol (Use booking date)
    public String getDayDisplay() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
        String formattedDate = date.format(dateFormatter);
        String dayOfWeekStr = date.getDayOfWeek().toString();
        dayOfWeekStr = dayOfWeekStr.substring(0, 1).toUpperCase() + dayOfWeekStr.substring(1).toLowerCase(); // Capitalize
        return formattedDate + " (" + dayOfWeekStr + ")";
    }

    // For status display - statusCol, appStatusCol
    public String getStatusDisplay() { return status.toString(); }

    // Need equals/hashCode if comparing bookings directly (Keep as is)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(bookingId, booking.bookingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId);
    }

    @Override
    public String toString() { // For debugging
        return "Booking{" +
                "id='" + bookingId + '\'' +
                ", user='" + facultyUsername + '\'' + // Use username
                ", room='" + roomNumber + '\'' + // Use room details
                ", type='" + roomType + '\'' +
                ", date=" + date +
                ", time=" + getTimeScheduleDisplay() +
                ", status=" + status +
                '}';
    }
}


// --- Central Data Store (Now with File Persistence) ---
public class DataStore {

    // File Paths
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path ROOMS_FILE = DATA_DIR.resolve("rooms.txt");
    private static final Path BOOKINGS_FILE = DATA_DIR.resolve("bookings.txt");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.txt");

    // Data Structures (Keep as is)
    static final Map<String, User> users = new HashMap<>();
    static final List<RoomSchedule> roomSchedules = new ArrayList<>();
    static final List<Booking> bookings = new ArrayList<>();

    // File formatters (package-private for visibility from nested classes)
    // FIX: Made package-private for visibility from load methods
    static final DateTimeFormatter TIME_FILE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm"); // 24hr for easy parsing
    static final DateTimeFormatter DATE_FILE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD


    // Keep track of the logged-in user (Keep as is)
    private static User currentLoggedInUser;

    // Static initializer block - Used to LOAD data on startup
    static {
        ensureDataDirectoryExists();
        loadData();
        // Add a default admin if no users were loaded (for initial setup)
        // This relies on loadUsers() being called first in loadData()
        if (users.isEmpty()) {
            System.out.println("No users loaded, adding default admin...");
            // Use addUser which now saves to file
            boolean adminAdded = addUser("admin", "admin123", UserRole.ADMIN);
            if (!adminAdded) {
                System.err.println("Failed to add default admin user!");
            } else {
                System.out.println("Default admin added and saved.");
            }
        }
    }

    // --- File Persistence Methods ---

    private static void ensureDataDirectoryExists() {
        if (!Files.exists(DATA_DIR)) {
            try {
                Files.createDirectories(DATA_DIR);
                System.out.println("Created data directory: " + DATA_DIR.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating data directory: " + DATA_DIR.toAbsolutePath());
                e.printStackTrace();
                // In a real app, you might show a fatal error and exit
            }
        }
    }

    private static void loadData() {
        System.out.println("Loading data...");
        loadUsers(); // Load users first, as bookings refer to them
        loadRoomSchedules();
        loadBookings(); // Load bookings AFTER users
        System.out.println("Data loading complete.");
    }

    // Saves all data to respective files
    public static void saveData() {
        System.out.println("Saving data...");
        saveUsers(); // Save users too
        saveRoomSchedules();
        saveBookings();
        System.out.println("Data saving complete.");
    }

    // --- User Persistence ---
    private static final String USER_HEADER = "username|password|role";

    private static void loadUsers() {
        users.clear(); // Clear existing data
        if (!Files.exists(USERS_FILE)) {
            System.out.println("Users file not found: " + USERS_FILE.toAbsolutePath());
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(USERS_FILE)) {
            String header = reader.readLine(); // Read header
            if (header == null || !header.equals(USER_HEADER)) {
                System.err.println("Warning: Invalid users file header. Skipping load from " + USERS_FILE.toAbsolutePath());
                return;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1); // Use -1 limit to keep trailing empty strings
                if (parts.length == 3) { // username, password, role
                    try {
                        UserRole role = UserRole.valueOf(parts[2].toUpperCase()); // Ensure case sensitivity doesn't break loading
                        User user = new User(parts[0], parts[1], role); // WARNING: Loads plain text password!
                        users.put(user.getUsername(), user);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping invalid user line (bad role): " + line + " in " + USERS_FILE.toAbsolutePath());
                    }
                } else {
                    System.err.println("Skipping invalid user line (wrong number of fields): " + line + " in " + USERS_FILE.toAbsolutePath());
                }
            }
            System.out.println("Loaded " + users.size() + " users from " + USERS_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error reading users file: " + USERS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void saveUsers() {
        try (BufferedWriter writer = Files.newBufferedWriter(USERS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(USER_HEADER);
            writer.newLine();
            for (User user : users.values()) {
                // WARNING: Saving plain text password!
                writer.write(user.getUsername() + "|" + user.getPassword() + "|" + user.getRole().name()); // Use name() for enum
                writer.newLine();
            }
            System.out.println("Saved " + users.size() + " users to " + USERS_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing users file: " + USERS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }


    // --- Room Schedule Persistence ---
    private static final String SCHEDULE_HEADER = "scheduleId|roomNumber|roomType|startTime|endTime|dayOfWeek";
    // FIX: Formatters moved up to static fields with package-private access

    private static void loadRoomSchedules() {
        roomSchedules.clear(); // Clear existing data
        // FIX: Accessing the counter using the package-private modifier is now allowed
        RoomSchedule.idCounter.set(0); // Reset counter before loading

        if (!Files.exists(ROOMS_FILE)) {
            System.out.println("Room schedules file not found: " + ROOMS_FILE.toAbsolutePath());
            return;
        }

        // Defensive check - Should theoretically not be needed for a static final field
        // but added based on the NullPointerException stack trace.
        if (TIME_FILE_FORMATTER == null) {
            System.err.println("FATAL ERROR: TIME_FILE_FORMATTER is null during loadRoomSchedules!");
            // Optionally throw an exception or show a fatal error message box and exit
            // SceneNavigator.showAlert(Alert.AlertType.ERROR, "Data Loading Error", "Internal application error: Time formatter not initialized."); // Requires FX thread
            return; // Prevent NPE by stopping load
        }

        try (BufferedReader reader = Files.newBufferedReader(ROOMS_FILE)) {
            String header = reader.readLine(); // Read header
            if (header == null || !header.equals(SCHEDULE_HEADER)) {
                System.err.println("Warning: Invalid room schedules file header. Skipping load from " + ROOMS_FILE.toAbsolutePath());
                return;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1); // Use -1 limit
                if (parts.length == 6) { // id, num, type, start, end, day
                    try {
                        // parts[3] and parts[4] can be empty strings, which parse("") throws DateTimeParseException for
                        LocalTime startTime = LocalTime.parse(parts[3], TIME_FILE_FORMATTER);
                        LocalTime endTime = LocalTime.parse(parts[4], TIME_FILE_FORMATTER);
                        DayOfWeek dayOfWeek = DayOfWeek.valueOf(parts[5].toUpperCase()); // Ensure case sensitivity doesn't break
                        // Use the loading constructor
                        RoomSchedule schedule = new RoomSchedule(parts[0], parts[1], parts[2], startTime, endTime, dayOfWeek);
                        roomSchedules.add(schedule);

                    } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
                        System.err.println("Skipping invalid schedule line (parse error): " + line + " in " + ROOMS_FILE.toAbsolutePath() + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Skipping invalid schedule line (wrong number of fields): " + line + " in " + ROOMS_FILE.toAbsolutePath());
                }
            }
            // The RoomSchedule constructor for loading already updates the static counter
            System.out.println("Loaded " + roomSchedules.size() + " room schedules from " + ROOMS_FILE.toAbsolutePath() + ". Next Schedule ID: SCH-" + RoomSchedule.idCounter.get());
        } catch (IOException e) {
            System.err.println("Error reading room schedules file: " + ROOMS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void saveRoomSchedules() {
        try (BufferedWriter writer = Files.newBufferedWriter(ROOMS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(SCHEDULE_HEADER);
            writer.newLine();
            for (RoomSchedule schedule : roomSchedules) {
                writer.write(schedule.getScheduleId() + "|" +
                        schedule.getRoomNumber() + "|" +
                        schedule.getRoomType() + "|" +
                        schedule.getStartTime().format(TIME_FILE_FORMATTER) + "|" +
                        schedule.getEndTime().format(TIME_FILE_FORMATTER) + "|" +
                        schedule.getDayOfWeek().name()); // Use name() for enum
                writer.newLine();
            }
            System.out.println("Saved " + roomSchedules.size() + " room schedules to " + ROOMS_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing room schedules file: " + ROOMS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }


    // --- Booking Persistence ---
    private static final String BOOKING_HEADER = "bookingId|facultyUsername|roomNumber|roomType|date|bookedStartTime|bookedEndTime|status";
    // FIX: Formatters moved up to static fields

    private static void loadBookings() {
        bookings.clear(); // Clear existing data
        // FIX: Accessing the counter using the package-private modifier is now allowed
        Booking.idCounter.set(0); // Reset counter before loading

        if (!Files.exists(BOOKINGS_FILE)) {
            System.out.println("Bookings file not found: " + BOOKINGS_FILE.toAbsolutePath());
            return;
        }

        // Defensive checks for formatters
        if (TIME_FILE_FORMATTER == null) {
            System.err.println("FATAL ERROR: TIME_FILE_FORMATTER is null during loadBookings!");
            return;
        }
        if (DATE_FILE_FORMATTER == null) {
            System.err.println("FATAL ERROR: DATE_FILE_FORMATTER is null during loadBookings!");
            return;
        }


        try (BufferedReader reader = Files.newBufferedReader(BOOKINGS_FILE)) {
            String header = reader.readLine(); // Read header
            if (header == null || !header.equals(BOOKING_HEADER)) {
                System.err.println("Warning: Invalid bookings file header. Skipping load from " + BOOKINGS_FILE.toAbsolutePath());
                return;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1); // Use -1 limit
                if (parts.length == 8) { // id, user, roomNum, roomType, date, start, end, status
                    try {
                        // parts[4] for date, parts[5] for start time, parts[6] for end time
                        LocalDate date = LocalDate.parse(parts[4], DATE_FILE_FORMATTER);
                        LocalTime startTime = LocalTime.parse(parts[5], TIME_FILE_FORMATTER);
                        LocalTime endTime = LocalTime.parse(parts[6], TIME_FILE_FORMATTER);
                        BookingStatus status = BookingStatus.valueOf(parts[7].toUpperCase()); // Ensure case sensitivity doesn't break
                        String username = parts[1];

                        // Basic validation: check if the user exists before creating booking object
                        if(users.containsKey(username)) {
                            // Use the loading constructor
                            Booking booking = new Booking(parts[0], username, parts[2], parts[3], date, startTime, endTime, status);
                            bookings.add(booking);
                        } else {
                            System.err.println("Skipping booking line for non-existent user ('" + username + "'): " + line + " in " + BOOKINGS_FILE.toAbsolutePath());
                        }

                    } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
                        System.err.println("Skipping invalid booking line (parse error): " + line + " in " + BOOKINGS_FILE.toAbsolutePath() + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Skipping invalid booking line (wrong number of fields): " + line + " in " + BOOKINGS_FILE.toAbsolutePath());
                }
            }
            // The Booking constructor for loading already updates the static counter
            System.out.println("Loaded " + bookings.size() + " bookings from " + BOOKINGS_FILE.toAbsolutePath() + ". Next Booking ID: BOK-" + Booking.idCounter.get());
        } catch (IOException e) {
            System.err.println("Error reading bookings file: " + BOOKINGS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void saveBookings() {
        try (BufferedWriter writer = Files.newBufferedWriter(BOOKINGS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(BOOKING_HEADER);
            writer.newLine();
            for (Booking booking : bookings) {
                writer.write(booking.getBookingId() + "|" +
                        booking.getFacultyUsername() + "|" + // Use username
                        booking.getRoomNumber() + "|" +    // Use stored room details
                        booking.getRoomType() + "|" +
                        booking.getDate().format(DATE_FILE_FORMATTER) + "|" +
                        booking.getBookedStartTime().format(TIME_FILE_FORMATTER) + "|" +
                        booking.getBookedEndTime().format(TIME_FILE_FORMATTER) + "|" +
                        booking.getStatus().name()); // Use name() for enum
                writer.newLine();
            }
            System.out.println("Saved " + bookings.size() + " bookings to " + BOOKINGS_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing bookings file: " + BOOKINGS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }


    // --- User Management --- (Add getUser/containsUser for Booking lookup)
    public static User authenticateUser(String username, String password) {
        User user = users.get(username);
        // WARNING: NEVER compare passwords like this in production! Use a hashing library (e.g., bcrypt).
        if (user != null && user.getPassword().equals(password)) {
            setLoggedInUser(user); // Set logged in user on successful auth
            return user;
        }
        setLoggedInUser(null); // Ensure user is null on failed auth
        return null; // Authentication failed
    }

    // Added helper for Booking lookup
    public static User getUserByUsername(String username) {
        return users.get(username);
    }

    // Added helper for Booking lookup validation during load
    public static boolean containsUser(String username) {
        return users.containsKey(username);
    }


    // Modified addUser to save
    public static boolean addUser(String username, String password, UserRole role) {
        if (username == null || username.trim().isEmpty() || users.containsKey(username)) {
            System.out.println("Add user failed: Invalid or duplicate username '" + username + "'");
            return false; // Invalid or duplicate username
        }
        // Add password complexity rules in a real app
        User newUser = new User(username, password, role);
        users.put(username, newUser);
        System.out.println("User added: " + username + " Role: " + role); // Debugging
        saveUsers(); // Save changes
        return true;
    }

    public static void setLoggedInUser(User user) {
        currentLoggedInUser = user;
        System.out.println("User logged in: " + (user != null ? user.getUsername() : "null")); // Debugging
    }

    public static User getLoggedInUser() {
        return currentLoggedInUser;
    }

    public static void logout() {
        System.out.println("User logged out: " + (currentLoggedInUser != null ? currentLoggedInUser.getUsername() : "null")); // Debugging
        currentLoggedInUser = null;
    }

    // --- Schedule Management (Admin) ---

    // Modified addRoomSchedule to include conflict checking and save
    public static boolean addRoomSchedule(RoomSchedule schedule) {
        if (schedule == null) {
            System.out.println("Add room schedule failed: Schedule object is null.");
            return false;
        }
        // Check if a schedule for the same room and day already exists and overlaps
        boolean conflict = roomSchedules.stream()
                .filter(s -> s.getRoomNumber().equals(schedule.getRoomNumber())
                        && s.getDayOfWeek().equals(schedule.getDayOfWeek()))
                .anyMatch(existing ->
                        // Check for time overlap: (StartA < EndB) and (EndA > StartB)
                        // This correctly handles cases where schedules start/end at the same time
                        // e.g., [9, 10) and [10, 11) do NOT conflict. [9, 10) and [9:30, 10:30) DO conflict.
                        schedule.getStartTime().isBefore(existing.getEndTime()) &&
                                schedule.getEndTime().isAfter(existing.getStartTime())
                );

        if (conflict) {
            System.out.println("Room schedule conflict detected for " + schedule.getRoomNumber() + " on " + schedule.getDayOfWeek() + " from " + schedule.getStartTime() + " to " + schedule.getEndTime());
            return false; // Conflict detected, do not add
        }

        roomSchedules.add(schedule);
        System.out.println("Schedule added: " + schedule); // Debugging
        saveRoomSchedules(); // Save changes
        return true; // Schedule added successfully
    }

    public static List<RoomSchedule> getAllRoomSchedules() {
        return new ArrayList<>(roomSchedules); // Return a copy to prevent external modification
    }

    // Modified removeRoomSchedule to save
    public static boolean removeRoomSchedule(String scheduleId) {
        boolean removed = roomSchedules.removeIf(schedule -> schedule.getScheduleId().equals(scheduleId));
        if(removed) {
            System.out.println("Schedule removed: " + scheduleId); // Debugging
            saveRoomSchedules(); // Save changes
        } else {
            System.out.println("Remove schedule failed: Schedule ID " + scheduleId + " not found.");
        }
        return removed;
    }

    public static RoomSchedule findScheduleById(String scheduleId) {
        if (scheduleId == null) return null;
        return roomSchedules.stream()
                .filter(s -> scheduleId.equals(s.getScheduleId()))
                .findFirst()
                .orElse(null);
    }

    // --- Booking Management ---

    // Modified addBooking to save (existing conflict/duration checks remain)
    public static boolean addBooking(Booking booking) {
        if (booking == null) {
            System.out.println("Add booking failed: Booking object is null.");
            return false;
        }

        // CRITICAL: Check for conflicts with APPROVED bookings before adding
        if (isTimeSlotBooked(booking.getRoomNumber(), booking.getDate(), booking.getBookedStartTime(), booking.getBookedEndTime())) {
            System.out.println("Booking conflict detected with existing APPROVED booking for " + booking.getRoomNumber() + " on " + booking.getDate() + " from " + booking.getBookedStartTime() + " to " + booking.getBookedEndTime()); // Debugging
            return false; // Conflict detected
        }

        // Check 3-hour limit (Duration between start and end time)
        Duration bookingDuration = Duration.between(booking.getBookedStartTime(), booking.getBookedEndTime());
        // Use Minutes to be precise, ensure it's not > 3 hours
        if (bookingDuration.toMinutes() > Duration.ofHours(3).toMinutes()) {
            System.out.println("Booking rejected (exceeds 3 hours): " + bookingDuration.toMinutes() + " minutes for " + booking); // Debugging
            return false; // Exceeds limit
        }
        // Also check minimum duration? (e.g. 15 mins)
        if (bookingDuration.toMinutes() < 15) { // Example minimum
            System.out.println("Booking rejected (less than minimum duration): " + bookingDuration.toMinutes() + " minutes for " + booking);
            return false;
        }


        bookings.add(booking);
        System.out.println("Booking added (Pending): " + booking); // Debugging
        saveBookings(); // Save changes
        return true;
    }

    public static List<Booking> getAllBookings() {
        return new ArrayList<>(bookings); // Return a copy
    }

    public static List<Booking> getBookingsByUser(User user) {
        if (user == null) return new ArrayList<>();
        // Filter using the stored username
        return bookings.stream()
                .filter(b -> user.getUsername().equals(b.getFacultyUsername()))
                .collect(Collectors.toList());
    }

    public static List<Booking> getBookingsByStatus(BookingStatus status) {
        if (status == null) return new ArrayList<>();
        return bookings.stream()
                .filter(b -> b.getStatus() == status)
                .collect(Collectors.toList());
    }

    public static Booking findBookingById(String bookingId) {
        if (bookingId == null) return null;
        return bookings.stream()
                .filter(b -> bookingId.equals(b.getBookingId())) // Safe comparison
                .findFirst()
                .orElse(null);
    }

    // Checks if a slot conflicts with existing APPROVED bookings
    public static boolean isTimeSlotBooked(String roomNumber, LocalDate date, LocalTime proposedStartTime, LocalTime proposedEndTime) {
        // Basic validation
        if (roomNumber == null || date == null || proposedStartTime == null || proposedEndTime == null) {
            System.err.println("isTimeSlotBooked called with null parameters.");
            return true; // Assume conflict on invalid input
        }
        // Check if proposedEndTime is not after proposedStartTime - should be validated earlier, but as a safeguard
        if (!proposedEndTime.isAfter(proposedStartTime)) {
            System.err.println("isTimeSlotBooked called with invalid time range: " + proposedStartTime + " to " + proposedEndTime);
            return true; // Invalid range counts as conflict
        }


        return bookings.stream()
                .anyMatch(existingBooking ->
                        existingBooking.getStatus() == BookingStatus.APPROVED && // Only check approved
                                existingBooking.getRoomNumber().equals(roomNumber) &&
                                existingBooking.getDate().equals(date) &&
                                // Check for overlap: (StartA < EndB) and (EndA > StartB)
                                proposedStartTime.isBefore(existingBooking.getBookedEndTime()) &&
                                proposedEndTime.isAfter(existingBooking.getBookedStartTime())
                );
    }

    // Helper method used by StaffViewAvailableRooms to check if a specific slot is free
    public static boolean isTimeSlotAvailable(String roomNumber, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // isTimeSlotBooked already includes null checks and basic range validation
        return !isTimeSlotBooked(roomNumber, date, startTime, endTime);
    }


    // --- Utility for Admin Home Page ---
    public static long getTotalRoomSchedules() {
        return roomSchedules.size();
    }

    public static long countSchedulesByType(String type) {
        if (type == null) return 0;
        return roomSchedules.stream()
                .filter(s -> type.equalsIgnoreCase(s.getRoomType()))
                .count();
    }

    // --- Utility for Staff View Available Rooms ---
    // Get distinct room numbers for ComboBox
    public static List<String> getDistinctRoomNumbers() {
        return roomSchedules.stream()
                .map(RoomSchedule::getRoomNumber)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Get distinct room types for ComboBox
    public static List<String> getDistinctRoomTypes() {
        return roomSchedules.stream()
                .map(RoomSchedule::getRoomType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}