package com.example.roomutilizationsystem;

import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// --- Enums ---
enum UserRole {
    ADMIN, FACULTY
}

enum BookingStatus {
    PENDING, APPROVED, REJECTED, CANCELLED // Added Cancelled
}

// --- Data Classes ---
class User {
    private String username;
    private String password;
    private UserRole role;

    public User(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; } // In a real app, hash this!
    public UserRole getRole() { return role; }
}

// Represents the availability defined by the Admin
class RoomSchedule {
    private String scheduleId; // Unique ID for the schedule entry
    private String roomNumber;
    private String roomType; // e.g., "Laboratory", "Lecture"
    private LocalTime startTime;
    private LocalTime endTime;
    private DayOfWeek dayOfWeek; // Use Java's DayOfWeek

    // Auto-generate ID
    private static AtomicLong idCounter = new AtomicLong();
    private static String createID() {
        return "SCH-" + String.valueOf(idCounter.getAndIncrement());
    }


    public RoomSchedule(String roomNumber, String roomType, LocalTime startTime, LocalTime endTime, DayOfWeek dayOfWeek) {
        this.scheduleId = createID();
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
    }

    // Getters
    public String getScheduleId() { return scheduleId; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }

    // For display
    public String getTimeRangeString() {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
        return startTime.format(formatter) + " - " + endTime.format(formatter);
    }

    @Override
    public String toString() { // For ComboBox display or debugging
        return roomNumber + " (" + roomType + ") " + dayOfWeek + " " + getTimeRangeString();
    }
}

// Represents an actual booking made by a Faculty/Staff member
class Booking {
    private String bookingId; // Unique ID
    private User facultyUser;
    private RoomSchedule roomSchedule; // Link to the schedule being booked
    private LocalDate date;
    private LocalTime bookedStartTime;
    private LocalTime bookedEndTime;
    private BookingStatus status;

    // Auto-generate ID
    private static AtomicLong idCounter = new AtomicLong();
    private static String createID() {
        return "BOK-" + String.valueOf(idCounter.getAndIncrement());
    }


    public Booking(User facultyUser, RoomSchedule roomSchedule, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime) {
        this.bookingId = createID();
        if (facultyUser.getRole() != UserRole.FACULTY) {
            throw new IllegalArgumentException("Only Faculty can create bookings.");
        }
        this.facultyUser = facultyUser;
        this.roomSchedule = roomSchedule;
        this.date = date;
        this.bookedStartTime = bookedStartTime;
        this.bookedEndTime = bookedEndTime;
        this.status = BookingStatus.PENDING; // Default status
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public User getFacultyUser() { return facultyUser; }
    public RoomSchedule getRoomSchedule() { return roomSchedule; }
    public LocalDate getDate() { return date; }
    public LocalTime getBookedStartTime() { return bookedStartTime; }
    public LocalTime getBookedEndTime() { return bookedEndTime; }
    public BookingStatus getStatus() { return status; }

    // Setter for status (used by Admin)
    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    // For display
    public String getBookedTimeRangeString() {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
        return bookedStartTime.format(formatter) + " - " + bookedEndTime.format(formatter);
    }

    // --- Derived properties for TableView ---
    public String getFacultyUsername() { return facultyUser.getUsername(); }
    public String getFacultyRole() { return facultyUser.getRole().toString();} // Or just "Faculty"
    public String getRoomNumber() { return roomSchedule.getRoomNumber(); }
    public String getRoomType() { return roomSchedule.getRoomType(); }
    public String getTimeScheduleDisplay() { return getBookedTimeRangeString();} // Display booked time
    public String getDayDisplay() { return date.getDayOfWeek().toString();} // Display specific day booked
    public String getStatusDisplay() { return status.toString(); }
}


// --- Central Data Store (Static In-Memory) ---
class DataStore {
    // Use maps for efficient lookups where possible
    static final Map<String, User> users = new HashMap<>();
    static final List<RoomSchedule> roomSchedules = new ArrayList<>(); // List is fine for schedules
    static final List<Booking> bookings = new ArrayList<>();       // List is fine for bookings

    // Keep track of the logged-in user
    private static User currentLoggedInUser;

    // Static initializer block for default data (runs once when class is loaded)
    static {
        // Add default admin
        User admin = new User("admin", "admin123", UserRole.ADMIN);
        users.put(admin.getUsername(), admin);

        // Add default faculty
        User faculty = new User("faculty", "pass", UserRole.FACULTY);
        users.put(faculty.getUsername(), faculty);

        // Add some sample room schedules (optional, for testing)
        try {
            roomSchedules.add(new RoomSchedule("R201", "Lecture", LocalTime.of(8, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY));
            roomSchedules.add(new RoomSchedule("R201", "Lecture", LocalTime.of(13, 0), LocalTime.of(16, 0), DayOfWeek.MONDAY));
            roomSchedules.add(new RoomSchedule("L305", "Laboratory", LocalTime.of(9, 30), LocalTime.of(12, 30), DayOfWeek.TUESDAY));
            roomSchedules.add(new RoomSchedule("L305", "Laboratory", LocalTime.of(14, 0), LocalTime.of(17, 0), DayOfWeek.WEDNESDAY));
            roomSchedules.add(new RoomSchedule("R400", "Lecture", LocalTime.of(8, 0), LocalTime.of(17, 0), DayOfWeek.FRIDAY)); // Long availability
        } catch (Exception e) {
            System.err.println("Error adding initial schedules: " + e.getMessage());
        }
    }

    // --- User Management ---
    public static User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) { // Plain text compare (BAD PRACTICE!)
            return user;
        }
        return null; // Authentication failed
    }

    public static boolean addUser(String username, String password, UserRole role) {
        if (users.containsKey(username)) {
            return false; // Username already exists
        }
        User newUser = new User(username, password, role);
        users.put(username, newUser);
        return true;
    }

    public static void setLoggedInUser(User user) {
        currentLoggedInUser = user;
    }

    public static User getLoggedInUser() {
        return currentLoggedInUser;
    }

    public static void logout() {
        currentLoggedInUser = null;
    }

    // --- Schedule Management (Admin) ---
    public static void addRoomSchedule(RoomSchedule schedule) {
        // Optional: Add validation to prevent duplicate schedules (same room, day, overlapping time)
        roomSchedules.add(schedule);
    }

    public static List<RoomSchedule> getAllRoomSchedules() {
        return new ArrayList<>(roomSchedules); // Return a copy
    }

    public static boolean removeRoomSchedule(String scheduleId) {
        return roomSchedules.removeIf(schedule -> schedule.getScheduleId().equals(scheduleId));
    }

    public static RoomSchedule findScheduleById(String scheduleId) {
        return roomSchedules.stream()
                .filter(s -> s.getScheduleId().equals(scheduleId))
                .findFirst()
                .orElse(null);
    }

    // --- Booking Management ---
    public static boolean addBooking(Booking booking) {
        // CRITICAL: Check for conflicts before adding
        if (isTimeSlotAvailable(booking.getRoomSchedule().getRoomNumber(), booking.getDate(), booking.getBookedStartTime(), booking.getBookedEndTime())) {
            bookings.add(booking);
            return true;
        }
        return false; // Conflict detected
    }

    public static List<Booking> getAllBookings() {
        return new ArrayList<>(bookings); // Return a copy
    }

    public static List<Booking> getBookingsByUser(User user) {
        return bookings.stream()
                .filter(b -> b.getFacultyUser().equals(user))
                .collect(Collectors.toList());
    }

    public static Booking findBookingById(String bookingId) {
        return bookings.stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    // Method to find available schedules based on criteria (for Faculty View)
    public static List<RoomSchedule> findAvailableSchedules(LocalDate date, DayOfWeek day, String roomNumberFilter, String roomTypeFilter, LocalTime desiredStartTime, Duration desiredDuration) {
        List<RoomSchedule> potentialSchedules = roomSchedules.stream()
                .filter(s -> s.getDayOfWeek() == day)
                .filter(s -> roomNumberFilter == null || roomNumberFilter.isEmpty() || s.getRoomNumber().equalsIgnoreCase(roomNumberFilter))
                .filter(s -> roomTypeFilter == null || roomTypeFilter.isEmpty() || s.getRoomType().equalsIgnoreCase(roomTypeFilter))
                // Check if the schedule window can accommodate the desired booking time
                .filter(s -> {
                    if (desiredStartTime == null || desiredDuration == null) return true; // If no time specified, show all matching day/room
                    LocalTime desiredEndTime = desiredStartTime.plus((TemporalAmount) desiredDuration);
                    // Check if desired slot is within the schedule's bounds
                    return !desiredStartTime.isBefore(s.getStartTime()) && !desiredEndTime.isAfter(s.getEndTime());
                })
                .collect(Collectors.toList());

        // Now, filter out schedules that have conflicting APPROVED bookings for the GIVEN DATE and TIME
        List<RoomSchedule> trulyAvailable = new ArrayList<>();
        for (RoomSchedule schedule : potentialSchedules) {
            LocalTime checkStartTime = (desiredStartTime != null) ? desiredStartTime : schedule.getStartTime();
            LocalTime checkEndTime = (desiredDuration != null && desiredStartTime != null) ? desiredStartTime.plus((TemporalAmount) desiredDuration) : schedule.getEndTime();

            if(isTimeSlotAvailable(schedule.getRoomNumber(), date, checkStartTime, checkEndTime)) {
                trulyAvailable.add(schedule);
            }
            // If no specific time was requested, we just check if *any* part of the schedule is available
            // This part is tricky - maybe simplify to just show all matching schedules and handle conflict *on booking attempt*
            // For now, let's simplify: if a specific time isn't requested, we show the schedule if it matches day/room/type.
            // The faculty user then needs to pick a time slot when booking.
            else if (desiredStartTime == null) {
                // Check if *any* part of the schedule is available on that day (complex check not implemented here for brevity)
                // Simplification: Show the schedule template, conflict check happens on booking attempt.
                trulyAvailable.add(schedule);
            }
        }

        // return potentialSchedules; // Return potential ones, check conflict on booking.
        return trulyAvailable; // Return only ones verified available for the specific time (if provided)
    }

    // Crucial helper method to check for booking conflicts
    public static boolean isTimeSlotAvailable(String roomNumber, LocalDate date, LocalTime startTime, LocalTime endTime) {
        for (Booking existingBooking : bookings) {
            if (existingBooking.getStatus() == BookingStatus.APPROVED && // Only check approved bookings
                    existingBooking.getRoomSchedule().getRoomNumber().equals(roomNumber) &&
                    existingBooking.getDate().equals(date)) {

                // Check for overlap: (StartA < EndB) and (EndA > StartB)
                if (startTime.isBefore(existingBooking.getBookedEndTime()) && endTime.isAfter(existingBooking.getBookedStartTime())) {
                    return false; // Conflict found
                }
            }
        }
        return true; // No conflicts found
    }

    // --- Utility for Admin Home Page ---
    public static long getTotalRoomSchedules() {
        return roomSchedules.size();
    }

    public static long countSchedulesByType(String type) {
        return roomSchedules.stream()
                .filter(s -> s.getRoomType().equalsIgnoreCase(type))
                .count();
    }
}

/*// Utility class for Navigation
class SceneNavigator {
    public static void navigateTo(javafx.event.ActionEvent event, String fxmlFile) throws java.io.IOException {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(SceneNavigator.class.getResource(fxmlFile)); // Ensure path is correct
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
    }

    public static void showAlert(javafx.scene.control.Alert.AlertType type, String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}*/
