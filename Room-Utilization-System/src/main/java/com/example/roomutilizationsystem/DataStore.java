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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// --- Enums ---
enum UserRole { ADMIN, FACULTY, TEACHER, STAFF }
enum BookingStatus { PENDING, APPROVED, REJECTED, CANCELLED }

// --- Data Classes ---
class User {
    private String username;
    private String password;
    private UserRole role;
    public User(String u, String p, UserRole r) { username=u; password=p; role=r; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public UserRole getRole() { return role; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; User user = (User) o; return Objects.equals(username, user.username); }
    @Override public int hashCode() { return Objects.hash(username); }
}

// --- RoomSchedule Class (Updated with specificDate) ---
class RoomSchedule {
    private final String scheduleId;
    private String roomNumber;
    private String roomType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Set<DayOfWeek> daysOfWeek; // Stores the pattern (e.g., {MONDAY, WEDNESDAY, FRIDAY})
    private LocalDate definitionStartDate;
    private LocalDate definitionEndDate;
    // Removed specificDate

    static final AtomicLong idCounter = new AtomicLong(0);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    // Constructor for creating NEW schedule definitions
    public RoomSchedule(String roomNumber, String roomType, LocalTime startTime, LocalTime endTime,
                        Set<DayOfWeek> daysOfWeek,
                        LocalDate definitionStartDate, LocalDate definitionEndDate) {
        this.scheduleId = "SCH-" + idCounter.incrementAndGet();
        this.roomNumber = Objects.requireNonNull(roomNumber, "Room number cannot be null").trim();
        this.roomType = Objects.requireNonNull(roomType, "Room type cannot be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
        this.daysOfWeek = (daysOfWeek == null || daysOfWeek.isEmpty())
                ? EnumSet.noneOf(DayOfWeek.class) // Ensure it's never null, maybe throw error?
                : EnumSet.copyOf(daysOfWeek); // Use EnumSet for efficiency
        this.definitionStartDate = Objects.requireNonNull(definitionStartDate, "Definition start date cannot be null");
        this.definitionEndDate = Objects.requireNonNull(definitionEndDate, "Definition end date cannot be null");

        if (!this.daysOfWeek.isEmpty() && definitionStartDate.isAfter(definitionEndDate)) {
            throw new IllegalArgumentException("Definition End Date cannot be before Start Date.");
        }
        if (startTime.equals(endTime) || startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (this.daysOfWeek.isEmpty()) {
            System.err.println("Warning: Creating RoomSchedule definition with no days selected for ID " + scheduleId);
            // Consider throwing an IllegalArgumentException if no days are allowed
        }
    }

    // Constructor for LOADING existing schedule definitions
    public RoomSchedule(String scheduleId, String roomNumber, String roomType, LocalTime startTime, LocalTime endTime,
                        Set<DayOfWeek> daysOfWeek,
                        LocalDate definitionStartDate, LocalDate definitionEndDate) {
        this.scheduleId = Objects.requireNonNull(scheduleId, "Schedule ID cannot be null");
        this.roomNumber = Objects.requireNonNull(roomNumber, "Room number cannot be null").trim();
        this.roomType = Objects.requireNonNull(roomType, "Room type cannot be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
        this.daysOfWeek = (daysOfWeek == null || daysOfWeek.isEmpty())
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(daysOfWeek);
        this.definitionStartDate = Objects.requireNonNull(definitionStartDate, "Definition start date cannot be null for loaded " + scheduleId);
        this.definitionEndDate = Objects.requireNonNull(definitionEndDate, "Definition end date cannot be null for loaded " + scheduleId);

        // Basic validation on load
        if (!this.daysOfWeek.isEmpty() && definitionStartDate.isAfter(definitionEndDate)) {
            System.err.println("Warning: Loaded schedule definition "+scheduleId+" has end date before start date.");
        }
        if (startTime.equals(endTime) || startTime.isAfter(endTime)) {
            System.err.println("Warning: Loaded schedule definition "+scheduleId+" has end time not after start time.");
        }
        if (this.daysOfWeek.isEmpty()) {
            System.err.println("Warning: Loaded schedule definition "+scheduleId+" has no days of week.");
        }

        // Update ID counter safely
        try {
            String idNumStr = scheduleId.replace("SCH-", "");
            if (!idNumStr.isEmpty()) {
                long idNum = Long.parseLong(idNumStr);
                idCounter.updateAndGet(current -> Math.max(current, idNum)); // Ensure next ID is greater
            }
        } catch (NumberFormatException e) {
            System.err.println("Could not parse schedule ID: " + scheduleId + " for counter update.");
            // Potentially log this, but don't crash loading
        }
    }

    // Getters
    public String getScheduleId() { return scheduleId; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Set<DayOfWeek> getDaysOfWeek() { return Collections.unmodifiableSet(daysOfWeek); } // Return unmodifiable view
    public LocalDate getDefinitionStartDate() { return definitionStartDate; }
    public LocalDate getDefinitionEndDate() { return definitionEndDate; }

    // Derived Getters for Display
    public String getTimeRangeString() { return startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter); }
    public String getTimeColDisplay() { return getTimeRangeString(); }

    // Formats the Set of days into a sorted, short string like "Mon, Wed, Fri"
    public String getDaysOfWeekDisplay() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return "N/A";
        }
        // Sort days MON-SUN before displaying
        return daysOfWeek.stream()
                .sorted(Comparator.naturalOrder()) // DayOfWeek enum order is correct
                .map(day -> day.toString().substring(0, 3)) // Get "MON", "TUE", etc.
                .map(dayStr -> dayStr.charAt(0) + dayStr.substring(1).toLowerCase()) // Format as "Mon", "Tue"
                .collect(Collectors.joining(", "));
    }

    // Renamed from getBookingTypeDisplay - indicates if it's defined for multiple days or a range
    public String getDefinitionTypeDisplay() {
        if (daysOfWeek == null) return "Invalid";
        boolean spansMultipleDaysOrDates = daysOfWeek.size() > 1 || !definitionStartDate.equals(definitionEndDate);
        return spansMultipleDaysOrDates ? "Recurring" : "One Time";
    }

    // Renamed from getEndDateDisplay
    public String getDefinitionEndDateDisplay() {
        return (definitionEndDate != null) ? definitionEndDate.format(dateFormatter) : "N/A";
    }

    // Added for clarity in AdminViewRooms
    public String getDefinitionStartDateDisplay() {
        return (definitionStartDate != null) ? definitionStartDate.format(dateFormatter) : "N/A";
    }

    // Standard overrides
    @Override
    public String toString() {
        return "ScheduleDef{id='" + scheduleId +
                "', room='" + roomNumber +
                "', days=" + getDaysOfWeekDisplay() +
                ", time=" + getTimeRangeString() +
                ", range=" + getDefinitionStartDateDisplay() + " to " + getDefinitionEndDateDisplay() +
                '}';
    }

    @Override
    public boolean equals(Object o) { // Equality based on unique ID
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomSchedule that = (RoomSchedule) o;
        return Objects.equals(scheduleId, that.scheduleId);
    }

    @Override
    public int hashCode() { // HashCode based on unique ID
        return Objects.hash(scheduleId);
    }

    // Utility method to check if this definition covers a specific date and time slot
    public boolean covers(LocalDate date, LocalTime time) {
        if (date == null || time == null || daysOfWeek == null || daysOfWeek.isEmpty()) return false;

        // Check if date is within the definition's start/end range (inclusive)
        boolean dateInRange = !date.isBefore(definitionStartDate) && !date.isAfter(definitionEndDate);
        if (!dateInRange) return false;

        // Check if the date's day of the week is in the set
        boolean dayMatches = daysOfWeek.contains(date.getDayOfWeek());
        if (!dayMatches) return false;

        // Check if the time is within the definition's start/end range (inclusive start, exclusive end)
        boolean timeInRange = !time.isBefore(startTime) && time.isBefore(endTime); // time >= start && time < end
        return timeInRange;
    }

    // Utility method to check if this definition applies on a specific date (ignoring time)
    public boolean appliesOnDate(LocalDate date) {
        if (date == null || daysOfWeek == null || daysOfWeek.isEmpty()) return false;
        boolean dateInRange = !date.isBefore(definitionStartDate) && !date.isAfter(definitionEndDate);
        if (!dateInRange) return false;
        return daysOfWeek.contains(date.getDayOfWeek());
    }
}


// --- Booking Class (No changes needed from previous combined version) ---
class Booking {
    private final String bookingId;
    private String facultyUsername;
    private String roomNumber;
    private String roomType;
    private LocalDate date; // Specific date of the booking
    private LocalTime bookedStartTime;
    private LocalTime bookedEndTime;
    private BookingStatus status;
    static final AtomicLong idCounter = new AtomicLong(0);

    // Constructor for NEW Bookings
    public Booking(User bookerUser, RoomSchedule roomSchedule, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime) {
        this.bookingId = "BOK-" + idCounter.incrementAndGet();
        if (bookerUser == null || bookerUser.getRole() == UserRole.ADMIN) { throw new IllegalArgumentException("Invalid user role for booking."); }
        if (roomSchedule == null || date == null || bookedStartTime == null || bookedEndTime == null) { throw new IllegalArgumentException("Booking details cannot be null."); }
        if (!bookedEndTime.isAfter(bookedStartTime)) { throw new IllegalArgumentException("Booking end time must be after start time."); }
        this.facultyUsername = bookerUser.getUsername();
        this.roomNumber = roomSchedule.getRoomNumber(); // Copy details
        this.roomType = roomSchedule.getRoomType();
        this.date = date; // Use the specific date provided for the booking
        this.bookedStartTime = bookedStartTime;
        this.bookedEndTime = bookedEndTime;
        this.status = BookingStatus.PENDING;
    }

    // Constructor for LOADING Bookings
    public Booking(String bookingId, String facultyUsername, String roomNumber, String roomType, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime, BookingStatus status) {
        this.bookingId = bookingId;
        this.facultyUsername = facultyUsername;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.date = date;
        this.bookedStartTime = bookedStartTime;
        this.bookedEndTime = bookedEndTime;
        this.status = status;
        // Update ID counter
        try { String idNumStr = bookingId.replace("BOK-", ""); if (!idNumStr.isEmpty()) { long idNum = Long.parseLong(idNumStr); idCounter.updateAndGet(current -> Math.max(current, idNum + 1)); } }
        catch (NumberFormatException e) { System.err.println("Could not parse booking ID: " + bookingId + " for counter update."); }
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public User getBookerUser() { return DataStore.getUserByUsername(facultyUsername); }
    public String getFacultyUsername() { return facultyUsername; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalDate getDate() { return date; } // The date of the booking itself
    public LocalTime getBookedStartTime() { return bookedStartTime; }
    public LocalTime getBookedEndTime() { return bookedEndTime; }
    public BookingStatus getStatus() { return status; }

    // Setter
    public void setStatus(BookingStatus status) {
        this.status = Objects.requireNonNull(status);
        // Call save once after the status change, ideally triggered from the calling controller
        // or batch saved if multiple statuses are changed. For simplicity now, we keep it here.
        DataStore.saveBookings(); // Save all bookings whenever one changes status
    }
    // Derived properties
    public String getFacultyRole() { User user = getBookerUser(); return user != null ? user.getRole().toString() : "UNKNOWN"; }
    public String getTimeScheduleDisplay() { DateTimeFormatter f = DateTimeFormatter.ofPattern("h:mm a"); return bookedStartTime.format(f) + " - " + bookedEndTime.format(f); }
    public String getBookedTimeRangeString() { return getTimeScheduleDisplay(); }
    public String getDayDisplay() { DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE; String fd = date.format(df); String dow = date.getDayOfWeek().toString(); dow = dow.substring(0, 1).toUpperCase() + dow.substring(1).toLowerCase(); return fd + " (" + dow + ")"; }
    public String getStatusDisplay() { return status.toString(); }

    // Equals/HashCode
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Booking b = (Booking) o; return Objects.equals(bookingId, b.bookingId); }
    @Override public int hashCode() { return Objects.hash(bookingId); }

    // toString
    @Override public String toString() { return "Booking{id='" + bookingId + "', user='" + facultyUsername + "', room='" + roomNumber + "', type='" + roomType + "', date=" + date + ", time=" + getTimeScheduleDisplay() + ", status=" + status + '}'; }
}


// --- Central Data Store (With Corrected Schedule Persistence) ---
// --- Central Data Store (Implementing Definition Model) ---
public class DataStore {

    // File Paths
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path ROOMS_FILE = DATA_DIR.resolve("rooms.txt");
    private static final Path BOOKINGS_FILE = DATA_DIR.resolve("bookings.txt");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.txt");

    // Data Structures
    static final Map<String, User> users = new HashMap<>();
    // Should contain RoomSchedule objects read from RoomSchedule.java
    static final List<RoomSchedule> roomSchedules = new ArrayList<>(); // Holds DEFINITIONS
    static final List<Booking> bookings = new ArrayList<>(); // Holds individual booking instances

    // Formatters
    static final DateTimeFormatter TIME_FILE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter DATE_FILE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    static final String DAYS_OF_WEEK_SEPARATOR = ","; // Separator for file storage

    // Logged-in user state
    private static User currentLoggedInUser;

    // Static Initializer
    static {
        ensureDataDirectoryExists();
        loadData();
        if (users.isEmpty()) {
            System.out.println("No users loaded, adding default admin (admin/admin123)...");
            // Ensure addUser correctly handles saving
            addUser("admin", "admin123", UserRole.ADMIN);
        }
        // Reset ID counters based on loaded data MAX + 1
        resetIdCounters();
    }

    private static void resetIdCounters() {
        long maxScheduleId = roomSchedules.stream()
                .map(RoomSchedule::getScheduleId)
                .map(id -> id.replace("SCH-", ""))
                .mapToLong(numStr -> { try { return Long.parseLong(numStr); } catch (NumberFormatException e) { return 0L; } })
                .max().orElse(0L);
        // Use the static counter from the RoomSchedule class
        RoomSchedule.idCounter.set(maxScheduleId + 1);

        long maxBookingId = bookings.stream()
                .map(Booking::getBookingId)
                .map(id -> id.replace("BOK-", ""))
                .mapToLong(numStr -> { try { return Long.parseLong(numStr); } catch (NumberFormatException e) { return 0L; } })
                .max().orElse(0L);
        // Use the static counter from the Booking class
        Booking.idCounter.set(maxBookingId + 1);

        System.out.println("Reset counters: Next Schedule ID = SCH-" + RoomSchedule.idCounter.get() + ", Next Booking ID = BOK-" + Booking.idCounter.get());
    }

    // --- File/Data Management ---
    private static void ensureDataDirectoryExists() {
        if (!Files.exists(DATA_DIR)) {
            try {
                Files.createDirectories(DATA_DIR);
                System.out.println("Created data directory: " + DATA_DIR.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("FATAL: Error creating data directory: " + DATA_DIR.toAbsolutePath());
                e.printStackTrace();
                // Consider exiting if the data directory is essential and cannot be created
                // System.exit(1);
            }
        }
    }

    private static synchronized void loadData() {
        System.out.println("Loading data from .txt files...");
        loadUsers();
        loadRoomSchedules(); // Uses updated logic below
        loadBookings();
        System.out.println("Data loading complete.");
        // Reset counters AFTER loading is complete
        resetIdCounters();
    }

    // Save ALL data types
    public static synchronized void saveData() {
        System.out.println("Saving all data...");
        saveUsers();
        saveRoomSchedules();
        saveBookings();
        System.out.println("All data saved.");
    }

    // Save specific data types (useful after bulk operations or specific changes)
    public static synchronized void saveRoomSchedulesOnly() {
        System.out.println("Saving room schedules only...");
        saveRoomSchedules();
        System.out.println("Room schedules saved.");
    }

    public static synchronized void saveBookingsOnly() {
        System.out.println("Saving bookings only...");
        saveBookings();
        System.out.println("Bookings saved.");
    }

    public static synchronized void saveUsersOnly() {
        System.out.println("Saving users only...");
        saveUsers();
        System.out.println("Users saved.");
    }


    // --- User Persistence ---
    private static final String USER_HEADER = "username|password|role";

    private static void loadUsers() {
        users.clear();
        if (!Files.exists(USERS_FILE)) {
            System.out.println("Users file not found: " + USERS_FILE.toAbsolutePath());
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(USERS_FILE)) {
            String header = reader.readLine();
            if (header == null || !header.equals(USER_HEADER)) {
                System.err.println("Warning: Invalid users file header in " + USERS_FILE.toAbsolutePath() + ". Expected: '" + USER_HEADER + "', Found: '" + header + "'");
                return;
            }
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String[] parts = line.split("\\|", -1); // Split, keeping trailing empty strings
                if (parts.length == 3) {
                    try {
                        String username = parts[0].trim();
                        String password = parts[1]; // Don't trim password
                        UserRole role = UserRole.valueOf(parts[2].trim().toUpperCase());
                        if (username.isEmpty()) {
                            System.err.println("Skipping user on line " + lineNum + " (empty username): " + line);
                            continue;
                        }
                        User user = new User(username, password, role);
                        users.put(user.getUsername(), user);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping invalid user line " + lineNum + " (bad role): " + line);
                    } catch (Exception e) {
                        System.err.println("Skipping invalid user line " + lineNum + " (error parsing): " + line + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Skipping invalid user line " + lineNum + " (wrong field count, expected 3): " + line);
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
                writer.write(user.getUsername() + "|" + user.getPassword() + "|" + user.getRole().name());
                writer.newLine();
            }
            // System.out.println("Saved " + users.size() + " users to " + USERS_FILE.toAbsolutePath()); // Moved to saveUsersOnly for clarity
        } catch (IOException e) {
            System.err.println("Error writing users file: " + USERS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }

    // --- Room Schedule Definition Persistence ---
    private static final String SCHEDULE_HEADER = "scheduleId|roomNumber|roomType|startTime|endTime|daysOfWeek|definitionStartDate|definitionEndDate";

    private static void loadRoomSchedules() {
        roomSchedules.clear();
        RoomSchedule.idCounter.set(0); // Reset counter before load
        if (!Files.exists(ROOMS_FILE)) { System.out.println("Room schedules file not found: " + ROOMS_FILE.toAbsolutePath() + ". No schedules loaded."); return; }
        if (TIME_FILE_FORMATTER == null || DATE_FILE_FORMATTER == null) { System.err.println("FATAL: Time/Date formatter is null!"); return; }

        try (BufferedReader reader = Files.newBufferedReader(ROOMS_FILE)) {
            String header = reader.readLine();
            if (header == null || !header.equals(SCHEDULE_HEADER)) {
                System.err.println("Warning: Invalid/Outdated room schedules file header in " + ROOMS_FILE.toAbsolutePath() + ". Skipping load.");
                System.err.println("Expected: " + SCHEDULE_HEADER);
                System.err.println("Found:    " + (header != null ? header : "<null>"));
                return;
            }
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String[] parts = line.split("\\|", -1);
                if (parts.length == 8) { // Expect 8 parts for the definition model
                    try {
                        String scheduleId = parts[0].trim();
                        String roomNumber = parts[1].trim();
                        String roomType = parts[2].trim();
                        LocalTime startTime = LocalTime.parse(parts[3].trim(), TIME_FILE_FORMATTER);
                        LocalTime endTime = LocalTime.parse(parts[4].trim(), TIME_FILE_FORMATTER);

                        // Parse daysOfWeek (index 5) - comma-separated
                        Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
                        String daysStr = parts[5].trim();
                        if (!daysStr.isEmpty()) {
                            String[] dayNames = daysStr.split(DAYS_OF_WEEK_SEPARATOR);
                            for (String dayName : dayNames) {
                                try {
                                    if (!dayName.trim().isEmpty()) {
                                        daysOfWeek.add(DayOfWeek.valueOf(dayName.trim().toUpperCase()));
                                    }
                                } catch (IllegalArgumentException e) {
                                    System.err.println("Skipping invalid DayOfWeek '" + dayName + "' in schedule line " + lineNum + ": " + line);
                                }
                            }
                        }

                        LocalDate defStartDate = LocalDate.parse(parts[6].trim(), DATE_FILE_FORMATTER);
                        LocalDate defEndDate = LocalDate.parse(parts[7].trim(), DATE_FILE_FORMATTER);

                        // Use LOADING constructor for RoomSchedule definition
                        // Assumes RoomSchedule class is accessible (in same package or imported)
                        RoomSchedule schedule = new RoomSchedule(scheduleId, roomNumber, roomType, startTime, endTime,
                                daysOfWeek, defStartDate, defEndDate);
                        roomSchedules.add(schedule);

                    } catch (DateTimeParseException e) { System.err.println("Skipping invalid schedule definition line "+lineNum+" (parse error): " + line + " - " + e.getMessage()); }
                    catch (IllegalArgumentException e) { System.err.println("Skipping invalid schedule definition line "+lineNum+" (value error): " + line + " - " + e.getMessage()); }
                    catch (Exception e) { System.err.println("Skipping invalid schedule definition line "+lineNum+" (unexpected error): " + line + " - " + e.getMessage()); e.printStackTrace(); }
                } else { System.err.println("Skipping invalid schedule definition line "+lineNum+" (wrong field count, expected 8): " + line); }
            }
            System.out.println("Loaded " + roomSchedules.size() + " room schedule definitions from " + ROOMS_FILE.toAbsolutePath());
        } catch (IOException e) { System.err.println("Error reading room schedules file: " + ROOMS_FILE.toAbsolutePath()); e.printStackTrace(); }
        // Reset ID counter after loading all schedules
        // resetIdCounters(); // Moved to end of loadData
    }

    private static void saveRoomSchedules() {
        try (BufferedWriter writer = Files.newBufferedWriter(ROOMS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(SCHEDULE_HEADER);
            writer.newLine();
            int savedCount = 0;
            for (RoomSchedule schedule : roomSchedules) {
                String daysOfWeekStr = schedule.getDaysOfWeek().stream()
                        .sorted()
                        .map(Enum::name)
                        .collect(Collectors.joining(DAYS_OF_WEEK_SEPARATOR));
                String defStartDateStr = schedule.getDefinitionStartDate().format(DATE_FILE_FORMATTER);
                String defEndDateStr = schedule.getDefinitionEndDate().format(DATE_FILE_FORMATTER);

                writer.write(schedule.getScheduleId() + "|" +
                        schedule.getRoomNumber() + "|" +
                        schedule.getRoomType() + "|" +
                        schedule.getStartTime().format(TIME_FILE_FORMATTER) + "|" +
                        schedule.getEndTime().format(TIME_FILE_FORMATTER) + "|" +
                        daysOfWeekStr + "|" +
                        defStartDateStr + "|" +
                        defEndDateStr);
                writer.newLine();
                savedCount++;
            }
            // System.out.println("Saved " + savedCount + " room schedule definitions to " + ROOMS_FILE.toAbsolutePath()); // Moved to saveRoomSchedulesOnly
        } catch (IOException e) {
            System.err.println("Error writing room schedules file: " + ROOMS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }

    // --- Booking Persistence ---
    private static final String BOOKING_HEADER = "bookingId|facultyUsername|roomNumber|roomType|date|bookedStartTime|bookedEndTime|status";

    private static void loadBookings() {
        bookings.clear();
        Booking.idCounter.set(0); // Reset counter
        if (!Files.exists(BOOKINGS_FILE)) { System.out.println("Bookings file not found: " + BOOKINGS_FILE.toAbsolutePath()); return; }
        if (TIME_FILE_FORMATTER == null || DATE_FILE_FORMATTER == null) { System.err.println("FATAL: Formatters null in loadBookings"); return; }

        try (BufferedReader reader = Files.newBufferedReader(BOOKINGS_FILE)) {
            String header = reader.readLine();
            if (header == null || !header.equals(BOOKING_HEADER)) {
                System.err.println("Warning: Invalid bookings file header in " + BOOKINGS_FILE.toAbsolutePath() + ". Expected: '" + BOOKING_HEADER + "', Found: '" + header + "'");
                return;
            }
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String[] parts = line.split("\\|", -1);
                if (parts.length == 8) {
                    try {
                        String bookingId = parts[0].trim();
                        String username = parts[1].trim();
                        String roomNumber = parts[2].trim();
                        String roomType = parts[3].trim();
                        LocalDate date = LocalDate.parse(parts[4].trim(), DATE_FILE_FORMATTER);
                        LocalTime startTime = LocalTime.parse(parts[5].trim(), TIME_FILE_FORMATTER);
                        LocalTime endTime = LocalTime.parse(parts[6].trim(), TIME_FILE_FORMATTER);
                        BookingStatus status = BookingStatus.valueOf(parts[7].trim().toUpperCase());

                        if (users.containsKey(username)) {
                            Booking booking = new Booking(bookingId, username, roomNumber, roomType, date, startTime, endTime, status);
                            bookings.add(booking);
                        } else {
                            System.err.println("Skipping booking on line " + lineNum + " for non-existent user '" + username + "': " + line);
                        }
                    } catch (IllegalArgumentException | DateTimeParseException e) { System.err.println("Skipping invalid booking line "+lineNum+" (parse error): " + line + " - " + e.getMessage()); }
                    catch (Exception e) { System.err.println("Skipping invalid booking line "+lineNum+" (unexpected error): " + line + " - " + e.getMessage()); e.printStackTrace(); }
                } else { System.err.println("Skipping invalid booking line "+lineNum+" (wrong field count, expected 8): " + line); }
            }
            System.out.println("Loaded " + bookings.size() + " bookings from " + BOOKINGS_FILE.toAbsolutePath());
        } catch (IOException e) { System.err.println("Error reading bookings file: " + BOOKINGS_FILE.toAbsolutePath()); e.printStackTrace(); }
        // resetIdCounters(); // Moved to end of loadData
    }

    // Changed to package-private as it's called internally by Booking.setStatus or saveBookingsOnly/saveData
    static void saveBookings() {
        try (BufferedWriter writer = Files.newBufferedWriter(BOOKINGS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(BOOKING_HEADER);
            writer.newLine();
            for (Booking booking : bookings) {
                writer.write(booking.getBookingId() + "|" +
                        booking.getFacultyUsername() + "|" +
                        booking.getRoomNumber() + "|" +
                        booking.getRoomType() + "|" +
                        booking.getDate().format(DATE_FILE_FORMATTER) + "|" +
                        booking.getBookedStartTime().format(TIME_FILE_FORMATTER) + "|" +
                        booking.getBookedEndTime().format(TIME_FILE_FORMATTER) + "|" +
                        booking.getStatus().name());
                writer.newLine();
            }
            // System.out.println("Saved " + bookings.size() + " bookings to " + BOOKINGS_FILE.toAbsolutePath()); // Moved to saveBookingsOnly
        } catch (IOException e) {
            System.err.println("Error writing bookings file: " + BOOKINGS_FILE.toAbsolutePath());
            e.printStackTrace();
        }
    }

    // --- User Management Methods (Implementations) ---
    public static User authenticateUser(String u, String p) {
        User user = users.get(u);
        if (user != null && user.getPassword().equals(p)) {
            setLoggedInUser(user); // Set the logged-in user on successful authentication
            return user;
        }
        setLoggedInUser(null); // Ensure loggedInUser is null on failure
        return null; // Return null if authentication fails
    }

    public static User getUserByUsername(String u) {
        return users.get(u); // Return the user or null if not found
    }

    public static boolean containsUser(String u) {
        return users.containsKey(u); // Return true if username exists, false otherwise
    }

    public static synchronized boolean addUser(String u, String p, UserRole r) {
        // Basic validation
        if (u == null || u.trim().isEmpty() || p == null || p.isEmpty() || r == null) {
            System.err.println("Error adding user: Username, password, and role cannot be empty.");
            return false;
        }
        String trimmedUsername = u.trim();
        // Check if user already exists
        if (users.containsKey(trimmedUsername)) {
            System.err.println("Error adding user: Username '" + trimmedUsername + "' already exists.");
            return false;
        }
        // Add the user
        users.put(trimmedUsername, new User(trimmedUsername, p, r)); // Use trimmed username
        saveUsersOnly(); // Save the updated user list
        System.out.println("User added: " + trimmedUsername + " Role: " + r);
        return true;
    }

    // setLoggedInUser is void, no return needed
    public static void setLoggedInUser(User u) {
        currentLoggedInUser = u;
        System.out.println("User logged in: " + (u != null ? u.getUsername() + " [" + u.getRole() + "]" : "null"));
    }

    public static User getLoggedInUser() {
        return currentLoggedInUser; // Return the currently logged-in user (can be null)
    }

    // logout is void, no return needed
    public static void logout() {
        System.out.println("User logged out: " + (currentLoggedInUser != null ? currentLoggedInUser.getUsername() : "null"));
        currentLoggedInUser = null;
    }

    // --- Schedule Definition Management (Admin) ---
    public static synchronized boolean addRoomScheduleDefinition(RoomSchedule definitionToAdd) {
        if (definitionToAdd == null || definitionToAdd.getDaysOfWeek() == null) {
            System.err.println("Cannot add null schedule definition or one without daysOfWeek.");
            return false;
        }
        if (definitionToAdd.getDaysOfWeek().isEmpty()) {
            System.err.println("Warning: Adding schedule definition " + definitionToAdd.getScheduleId() + " with no days selected.");
            // Depending on requirements, might return false here. For now, allow it.
        }

        // *** Definition Overlap Check ***
        boolean conflict = roomSchedules.stream()
                .filter(existingDef -> existingDef.getRoomNumber().equalsIgnoreCase(definitionToAdd.getRoomNumber())) // Same Room
                .anyMatch(existingDef -> {
                    // 1. Date Range Overlap
                    boolean datesOverlap = definitionToAdd.getDefinitionStartDate().isBefore(existingDef.getDefinitionEndDate().plusDays(1)) &&
                            definitionToAdd.getDefinitionEndDate().isAfter(existingDef.getDefinitionStartDate().minusDays(1));
                    if (!datesOverlap) return false;
                    // 2. Time Range Overlap
                    boolean timesOverlap = definitionToAdd.getStartTime().isBefore(existingDef.getEndTime()) &&
                            definitionToAdd.getEndTime().isAfter(existingDef.getStartTime());
                    if (!timesOverlap) return false;
                    // 3. Day of Week Overlap
                    boolean daysOverlap = !Collections.disjoint(definitionToAdd.getDaysOfWeek(), existingDef.getDaysOfWeek());
                    return daysOverlap; // Conflict if dates AND times AND days overlap
                });

        if (conflict) {
            // CORRECTED: Use the definition's own formatting method getTimeRangeString()
            System.out.println("Schedule Definition conflict detected for Room " + definitionToAdd.getRoomNumber() +
                    " for days " + definitionToAdd.getDaysOfWeekDisplay() +
                    " between " + definitionToAdd.getTimeRangeString() + // Use existing helper
                    " within date range " + definitionToAdd.getDefinitionStartDateDisplay() + " to " + definitionToAdd.getDefinitionEndDateDisplay());

            // Find and print conflicting definition IDs
            List<RoomSchedule> conflicts = roomSchedules.stream()
                    .filter(existingDef -> existingDef.getRoomNumber().equalsIgnoreCase(definitionToAdd.getRoomNumber()))
                    .filter(existingDef -> {
                        boolean datesOverlap = definitionToAdd.getDefinitionStartDate().isBefore(existingDef.getDefinitionEndDate().plusDays(1)) &&
                                definitionToAdd.getDefinitionEndDate().isAfter(existingDef.getDefinitionStartDate().minusDays(1));
                        if (!datesOverlap) return false;
                        boolean timesOverlap = definitionToAdd.getStartTime().isBefore(existingDef.getEndTime()) &&
                                definitionToAdd.getEndTime().isAfter(existingDef.getStartTime());
                        if (!timesOverlap) return false;
                        boolean daysOverlap = !Collections.disjoint(definitionToAdd.getDaysOfWeek(), existingDef.getDaysOfWeek());
                        return daysOverlap;
                    }).collect(Collectors.toList());
            System.out.println("Conflicts with existing definition(s): " + conflicts.stream().map(RoomSchedule::getScheduleId).collect(Collectors.joining(", ")));

            return false;
        }

        // No conflict, add and save
        roomSchedules.add(definitionToAdd);
        System.out.println("Schedule Definition added: " + definitionToAdd);
        saveRoomSchedulesOnly(); // Save schedules after adding one
        return true;
    }

    // Get all definitions
    public static List<RoomSchedule> getAllRoomSchedules() {
        // Return a copy to prevent external modification of the internal list
        return new ArrayList<>(roomSchedules);
    }

    // Remove a definition by ID
    public static synchronized boolean removeRoomSchedule(String id) {
        boolean removed = roomSchedules.removeIf(s -> s.getScheduleId().equals(id));
        if(removed) {
            System.out.println("Schedule definition removed: " + id);
            saveRoomSchedulesOnly(); // Save after removal
        } else {
            System.out.println("Remove schedule definition failed: ID " + id + " not found.");
        }
        return removed;
    }

    // Find a definition by ID
    public static RoomSchedule findScheduleById(String id) {
        if (id == null) return null;
        return roomSchedules.stream().filter(s -> id.equals(s.getScheduleId())).findFirst().orElse(null);
    }

    // Find the definition that covers a specific date/time request
    public static Optional<RoomSchedule> findCoveringScheduleDefinition(String roomNumber, LocalDate date, LocalTime time) {
        if (roomNumber == null || date == null || time == null) return Optional.empty();
        return roomSchedules.stream()
                .filter(def -> def.getRoomNumber().equalsIgnoreCase(roomNumber))
                .filter(def -> def.appliesOnDate(date)) // Check date range and day of week
                // Check time range [start, end)
                .filter(def -> !time.isBefore(def.getStartTime()) && time.isBefore(def.getEndTime()))
                .findFirst(); // Return the first matching definition found
    }


    // --- Booking Management Methods ---

    // Add a SINGLE booking instance
    public static synchronized boolean addBooking(Booking bookingToAdd) {
        if (bookingToAdd == null) {
            System.err.println("Attempted to add a null booking.");
            return false;
        }

        // 1. Conflict Check (against APPROVED bookings)
        if (isTimeSlotBooked(bookingToAdd.getRoomNumber(), bookingToAdd.getDate(), bookingToAdd.getBookedStartTime(), bookingToAdd.getBookedEndTime())) {
            System.err.println("Booking Conflict: Room " + bookingToAdd.getRoomNumber() +
                    " on " + bookingToAdd.getDate() + " from " + bookingToAdd.getBookedStartTime() +
                    " to " + bookingToAdd.getBookedEndTime() + " is already booked (Approved).");
            return false;
        }

        // 2. Duration Validation
        Duration duration = Duration.between(bookingToAdd.getBookedStartTime(), bookingToAdd.getBookedEndTime());
        if (duration.toMinutes() < 15 || duration.toMinutes() > 180) { // Example limits
            System.err.println("Booking Invalid Duration for " + bookingToAdd.getBookingId() + ": " + duration.toMinutes() + " minutes. Must be between 15 and 180.");
            return false;
        }

        // 3. Availability Check (against admin definitions)
        Optional<RoomSchedule> coveringDef = findCoveringScheduleDefinition(
                bookingToAdd.getRoomNumber(), bookingToAdd.getDate(), bookingToAdd.getBookedStartTime()
        );
        if (coveringDef.isEmpty() || !bookingToAdd.getBookedEndTime().isAfter(bookingToAdd.getBookedStartTime()) || bookingToAdd.getBookedEndTime().isAfter(coveringDef.get().getEndTime())) {
            System.err.println("Booking Slot No Longer Available: " + bookingToAdd.getBookingId() + " at " +
                    bookingToAdd.getDate() + " " + bookingToAdd.getBookedTimeRangeString() +
                    " for room " + bookingToAdd.getRoomNumber() +
                    " does not fall within a currently defined and valid availability schedule (Start: " +
                    (coveringDef.isPresent()? coveringDef.get().getStartTime() : "N/A") + ", End: " +
                    (coveringDef.isPresent()? coveringDef.get().getEndTime() : "N/A") + ").");
            return false;
        }


        // No conflicts, add the booking
        bookings.add(bookingToAdd);
        System.out.println("Booking added (Pending): " + bookingToAdd);
        saveBookingsOnly(); // Save bookings after adding one
        return true;
    }


    // Get all individual booking instances
    public static List<Booking> getAllBookings() {
        // Return a copy
        return new ArrayList<>(bookings);
    }
    // Get bookings for a specific user
    public static List<Booking> getBookingsByUser(User u) {
        if (u == null) return new ArrayList<>();
        return bookings.stream()
                .filter(b -> u.getUsername().equals(b.getFacultyUsername()))
                .collect(Collectors.toList());
    }
    // Get bookings by status
    public static List<Booking> getBookingsByStatus(BookingStatus s) {
        if (s == null) return new ArrayList<>();
        return bookings.stream()
                .filter(b -> b.getStatus() == s)
                .collect(Collectors.toList());
    }
    // Find a specific booking instance by ID
    public static Booking findBookingById(String id) {
        if (id == null) return null;
        return bookings.stream().filter(b -> id.equals(b.getBookingId())).findFirst().orElse(null);
    }

    // Checks vs APPROVED bookings
    public static boolean isTimeSlotBooked(String roomNumber, LocalDate date, LocalTime requestedStartTime, LocalTime requestedEndTime) {
        if (roomNumber == null || date == null || requestedStartTime == null || requestedEndTime == null || !requestedEndTime.isAfter(requestedStartTime)) {
            System.err.println("Warning: Invalid input to isTimeSlotBooked.");
            return true; // Treat invalid input as a conflict
        }
        return bookings.stream()
                .filter(existingBooking -> existingBooking.getStatus() == BookingStatus.APPROVED)
                .filter(existingBooking -> existingBooking.getRoomNumber().equalsIgnoreCase(roomNumber))
                .filter(existingBooking -> existingBooking.getDate().equals(date))
                .anyMatch(existingBooking -> // Time overlap check: (StartA < EndB) && (EndA > StartB)
                        requestedStartTime.isBefore(existingBooking.getBookedEndTime()) &&
                                requestedEndTime.isAfter(existingBooking.getBookedStartTime())
                );
    }

    // Inverse of isTimeSlotBooked
    public static boolean isTimeSlotAvailable(String roomNumber, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return !isTimeSlotBooked(roomNumber, date, startTime, endTime);
    }


    // --- Utility Methods ---
    public static long getTotalRoomSchedules() { return roomSchedules.size(); }
    public static long countSchedulesByType(String t) { if (t == null) return 0; return roomSchedules.stream().filter(s -> t.equalsIgnoreCase(s.getRoomType())).count(); }
    public static List<String> getDistinctRoomNumbers() { return roomSchedules.stream().map(RoomSchedule::getRoomNumber).distinct().sorted().collect(Collectors.toList()); }
    public static List<String> getDistinctRoomTypes() {
        return roomSchedules.stream().map(RoomSchedule::getRoomType).filter(type -> "Laboratory".equalsIgnoreCase(type) || "Lecture".equalsIgnoreCase(type)).distinct().sorted().collect(Collectors.toList());
    }

} // End of DataStore class