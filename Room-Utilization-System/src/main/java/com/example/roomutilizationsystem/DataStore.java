package com.example.roomutilizationsystem;

// Keep existing imports: Duration, DayOfWeek, LocalDate, LocalTime, etc.
import java.time.Duration; // Explicitly add Duration if not auto-imported
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter; // For formatting display strings
import java.time.temporal.ChronoUnit; // For duration calculations if needed
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // For null checks
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.time.temporal.TemporalAmount; // Needed for LocalTime.plus


// --- Enums ---
enum UserRole {
    ADMIN, FACULTY // STAFF is represented by FACULTY here
}

enum BookingStatus {
    PENDING, APPROVED, REJECTED, CANCELLED // Added Cancelled
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

    // Static counter for unique IDs
    private static final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis()); // Start with timestamp for more uniqueness
    private static String createID() {
        return "SCH-" + idCounter.getAndIncrement();
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

    // Formatter for display
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    // For display in tables or lists
    public String getTimeRangeString() {
        return startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
    }

    // Derived property for TableView (matches Time Column in FXML)
    public String getTimeColDisplay() { return getTimeRangeString(); }

    // Derived property for TableView (matches Day Column in FXML)
    public String getDayColDisplay() {
        String dayStr = dayOfWeek.toString();
        return dayStr.substring(0, 1).toUpperCase() + dayStr.substring(1).toLowerCase();
    }


    @Override
    public String toString() { // For debugging or simple lists
        return roomNumber + " (" + roomType + ") " + getDayColDisplay() + " " + getTimeRangeString();
    }

    // Need equals/hashCode if comparing schedules directly
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
    private final String bookingId; // Unique ID
    private final User facultyUser;
    private final RoomSchedule roomSchedule; // Link to the schedule template used
    private final LocalDate date; // Specific date of booking
    private final LocalTime bookedStartTime; // Specific time booked
    private final LocalTime bookedEndTime;   // Specific time booked
    private BookingStatus status;

    // Static counter for unique IDs
    private static final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis() + 1000); // Offset from schedule counter
    private static String createID() {
        return "BOK-" + idCounter.getAndIncrement();
    }

    // Constructor
    public Booking(User facultyUser, RoomSchedule roomSchedule, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime) {
        this.bookingId = createID();
        if (facultyUser == null || facultyUser.getRole() != UserRole.FACULTY) {
            throw new IllegalArgumentException("Invalid user for booking. Must be Faculty.");
        }
        if (roomSchedule == null || date == null || bookedStartTime == null || bookedEndTime == null) {
            throw new IllegalArgumentException("Booking details cannot be null.");
        }
        if (bookedEndTime.isBefore(bookedStartTime) || bookedEndTime.equals(bookedStartTime)) {
            throw new IllegalArgumentException("Booking end time must be after start time.");
        }
        // Optional: Check if booked times are within the linked RoomSchedule's times? Might be redundant if checked elsewhere.

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

    // Setter for status (used by Admin or for cancellation)
    public void setStatus(BookingStatus status) {
        this.status = Objects.requireNonNull(status, "Booking status cannot be null");
    }

    // --- Derived properties for TableView Columns ---
    // Make sure these method names match the PropertyValueFactory strings in controllers

    public String getFacultyUsername() { return facultyUser.getUsername(); }

    // For AdminManageBookingsController - roleCol
    public String getFacultyRole() { return facultyUser.getRole().toString(); }

    // For multiple controllers - roomNoCol, penRoomNoCol, appRoomNoCol, availRoomNoCol
    public String getRoomNumber() { return roomSchedule.getRoomNumber(); }

    // For multiple controllers - roomTypeCol, penRoomTypeCol, appRoomTypeCol, availRoomTypeCol
    public String getRoomType() { return roomSchedule.getRoomType(); }

    // For time display in tables - timeCol, penTimeCol, appTimeCol
    public String getTimeScheduleDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return bookedStartTime.format(formatter) + " - " + bookedEndTime.format(formatter);
    }
    // Alias for consistency if needed (e.g., if FXML uses 'bookedTimeRangeString')
    public String getBookedTimeRangeString() { return getTimeScheduleDisplay(); }


    // For day display in tables - dayCol, penDayCol, appDayCol
    public String getDayDisplay() {
        String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE); // YYYY-MM-DD
        String dayOfWeekStr = date.getDayOfWeek().toString();
        dayOfWeekStr = dayOfWeekStr.substring(0, 1).toUpperCase() + dayOfWeekStr.substring(1).toLowerCase(); // Capitalize
        return formattedDate + " (" + dayOfWeekStr + ")";
    }

    // For status display - statusCol, appStatusCol
    public String getStatusDisplay() { return status.toString(); }

    // Need equals/hashCode if comparing bookings directly
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
                ", user=" + facultyUser.getUsername() +
                ", room='" + getRoomNumber() + '\'' +
                ", date=" + date +
                ", time=" + getTimeScheduleDisplay() +
                ", status=" + status +
                '}';
    }
}


// --- Central Data Store (Static In-Memory) ---
// WARNING: Not thread-safe. For concurrent access, use thread-safe collections
//          and synchronized methods or locks.
public class DataStore { // Changed visibility to public
    // Use maps for efficient user lookup
    static final Map<String, User> users = new HashMap<>();
    // Use lists for schedules and bookings (iteration is common)
    // Consider ConcurrentHashMap/CopyOnWriteArrayList for thread safety if needed
    static final List<RoomSchedule> roomSchedules = new ArrayList<>();
    static final List<Booking> bookings = new ArrayList<>();

    // Keep track of the logged-in user
    private static User currentLoggedInUser;

    // Static initializer block for default data
    static {
        // Add default admin
        User admin = new User("admin", "admin123", UserRole.ADMIN);
        users.put(admin.getUsername(), admin);

        // Add default faculty users (matching UI examples)
        User faculty1 = new User("Roche", "pass", UserRole.FACULTY);
        User faculty2 = new User("Poro", "pass", UserRole.FACULTY);
        User faculty3 = new User("Han", "pass", UserRole.FACULTY);
        users.put(faculty1.getUsername(), faculty1);
        users.put(faculty2.getUsername(), faculty2);
        users.put(faculty3.getUsername(), faculty3);
        // Add the generic 'faculty' user too if needed for testing
        users.put("faculty", new User("faculty", "pass", UserRole.FACULTY));

        // Add some sample room schedules matching UI examples
        try {
            // Corresponds to R203 in AdminView/Manage, StaffManage
            roomSchedules.add(new RoomSchedule("R203", "Lab", LocalTime.of(7, 30), LocalTime.of(10, 30), DayOfWeek.MONDAY));
            // Corresponds to R210 in AdminView/Manage, StaffManage
            roomSchedules.add(new RoomSchedule("R210", "Lec", LocalTime.of(12, 30), LocalTime.of(13, 30), DayOfWeek.FRIDAY)); // 1 hour duration
            // Corresponds to R211 in AdminView/Manage, StaffManage
            roomSchedules.add(new RoomSchedule("R211", "Lec", LocalTime.of(16, 30), LocalTime.of(17, 30), DayOfWeek.TUESDAY)); // 1 hour duration

            // Add schedules for R803 Lab from StaffView example
            roomSchedules.add(new RoomSchedule("R803", "Lab", LocalTime.of(7, 30), LocalTime.of(10, 30), DayOfWeek.MONDAY));
            roomSchedules.add(new RoomSchedule("R803", "Lab", LocalTime.of(11, 30), LocalTime.of(12, 30), DayOfWeek.MONDAY)); // 1hr
            roomSchedules.add(new RoomSchedule("R803", "Lab", LocalTime.of(13, 30), LocalTime.of(16, 30), DayOfWeek.MONDAY));
            roomSchedules.add(new RoomSchedule("R803", "Lab", LocalTime.of(16, 30), LocalTime.of(18, 30), DayOfWeek.MONDAY)); // 2hr
            roomSchedules.add(new RoomSchedule("R803", "Lab", LocalTime.of(18, 30), LocalTime.of(20, 30), DayOfWeek.MONDAY)); // 2hr

            // Add a Lecture room for testing
            roomSchedules.add(new RoomSchedule("R101", "Lecture", LocalTime.of(9, 0), LocalTime.of(12, 0), DayOfWeek.WEDNESDAY));

        } catch (Exception e) {
            System.err.println("Error adding initial schedules: " + e.getMessage());
            e.printStackTrace();
        }

        // Add some sample bookings matching UI examples (assuming today or a near future date)
        // These need corresponding schedules above to be valid
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY); // Find next Monday (or today if Monday)
        LocalDate friday = today.with(DayOfWeek.FRIDAY);
        LocalDate tuesday = today.with(DayOfWeek.TUESDAY);

        // Find schedules by ID or details (more robust)
        RoomSchedule r203Mon = findScheduleByDetails("R203", DayOfWeek.MONDAY, LocalTime.of(7, 30));
        RoomSchedule r210Fri = findScheduleByDetails("R210", DayOfWeek.FRIDAY, LocalTime.of(12, 30));
        RoomSchedule r211Tue = findScheduleByDetails("R211", DayOfWeek.TUESDAY, LocalTime.of(16, 30));

        try {
            if (r203Mon != null && users.get("Roche") != null) {
                Booking b1 = new Booking(users.get("Roche"), r203Mon, monday, LocalTime.of(7, 30), LocalTime.of(10, 30));
                b1.setStatus(BookingStatus.APPROVED); // As shown in StaffManage Approved
                bookings.add(b1);
            }
            if (r210Fri != null && users.get("Poro") != null) {
                Booking b2 = new Booking(users.get("Poro"), r210Fri, friday, LocalTime.of(12, 30), LocalTime.of(13, 30));
                b2.setStatus(BookingStatus.APPROVED); // As shown in StaffManage Approved
                bookings.add(b2);
            }
            if (r211Tue != null && users.get("Han") != null) {
                Booking b3 = new Booking(users.get("Han"), r211Tue, tuesday, LocalTime.of(16, 30), LocalTime.of(17, 30));
                b3.setStatus(BookingStatus.APPROVED); // As shown in StaffManage Approved
                bookings.add(b3);
            }
            // Add some PENDING bookings for StaffManage example
            if (r203Mon != null && users.get("Roche") != null) {
                Booking b4 = new Booking(users.get("Roche"), r203Mon, monday.plusWeeks(1), LocalTime.of(7, 30), LocalTime.of(10, 30));
                bookings.add(b4); // Status defaults to PENDING
            }
            if (r210Fri != null && users.get("Poro") != null) {
                Booking b5 = new Booking(users.get("Poro"), r210Fri, friday.plusWeeks(1), LocalTime.of(12, 30), LocalTime.of(13, 30));
                bookings.add(b5); // Status defaults to PENDING
            }
            if (r211Tue != null && users.get("Han") != null) {
                Booking b6 = new Booking(users.get("Han"), r211Tue, tuesday.plusWeeks(1), LocalTime.of(16, 30), LocalTime.of(17, 30));
                bookings.add(b6); // Status defaults to PENDING
            }

        } catch(Exception e) {
            System.err.println("Error adding initial bookings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper to find a schedule for initial data population
    private static RoomSchedule findScheduleByDetails(String roomNum, DayOfWeek day, LocalTime start) {
        return roomSchedules.stream()
                .filter(s -> s.getRoomNumber().equals(roomNum) && s.getDayOfWeek() == day && s.getStartTime().equals(start))
                .findFirst()
                .orElse(null);
    }


    // --- User Management ---
    public static User authenticateUser(String username, String password) {
        User user = users.get(username);
        // WARNING: NEVER compare passwords like this in production! Use a hashing library (e.g., bcrypt).
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null; // Authentication failed
    }

    public static boolean addUser(String username, String password, UserRole role) {
        if (username == null || username.trim().isEmpty() || users.containsKey(username)) {
            return false; // Invalid or duplicate username
        }
        // Add password complexity rules in a real app
        User newUser = new User(username, password, role);
        users.put(username, newUser);
        System.out.println("User added: " + username + " Role: " + role); // Debugging
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
    public static void addRoomSchedule(RoomSchedule schedule) {
        // Optional: Add validation to prevent duplicate schedules (same room, day, overlapping time)
        // This requires iterating through existing schedules for the same room/day and checking time overlap.
        if (schedule != null) {
            roomSchedules.add(schedule);
            System.out.println("Schedule added: " + schedule); // Debugging
        }
    }

    public static List<RoomSchedule> getAllRoomSchedules() {
        return new ArrayList<>(roomSchedules); // Return a copy to prevent external modification
    }

    public static boolean removeRoomSchedule(String scheduleId) {
        boolean removed = roomSchedules.removeIf(schedule -> schedule.getScheduleId().equals(scheduleId));
        if(removed) System.out.println("Schedule removed: " + scheduleId); // Debugging
        return removed;
    }

    public static RoomSchedule findScheduleById(String scheduleId) {
        return roomSchedules.stream()
                .filter(s -> s.getScheduleId().equals(scheduleId))
                .findFirst()
                .orElse(null);
    }

    // --- Booking Management ---
    public static boolean addBooking(Booking booking) {
        if (booking == null) return false;

        // CRITICAL: Check for conflicts with APPROVED bookings before adding
        // Note: PENDING bookings don't strictly block, but maybe they should show as 'tentative'?
        // For now, only check against APPROVED.
        if (isTimeSlotBooked(booking.getRoomNumber(), booking.getDate(), booking.getBookedStartTime(), booking.getBookedEndTime())) {
            System.out.println("Booking conflict detected: " + booking); // Debugging
            return false; // Conflict detected
        }

        // Check 3-hour limit (Duration between start and end time)
        Duration bookingDuration = Duration.between(booking.getBookedStartTime(), booking.getBookedEndTime());
        if (bookingDuration.toHours() > 3) {
            System.out.println("Booking rejected (exceeds 3 hours): " + booking); // Debugging
            return false; // Exceeds limit
        }

        bookings.add(booking);
        System.out.println("Booking added (Pending): " + booking); // Debugging
        return true;
    }

    public static List<Booking> getAllBookings() {
        return new ArrayList<>(bookings); // Return a copy
    }

    public static List<Booking> getBookingsByUser(User user) {
        if (user == null) return new ArrayList<>();
        return bookings.stream()
                .filter(b -> b.getFacultyUser().equals(user))
                .collect(Collectors.toList());
    }

    public static List<Booking> getBookingsByStatus(BookingStatus status) {
        if (status == null) return new ArrayList<>();
        return bookings.stream()
                .filter(b -> b.getStatus() == status)
                .collect(Collectors.toList());
    }

    public static Booking findBookingById(String bookingId) {
        return bookings.stream()
                .filter(b -> b.getBookingId() != null && b.getBookingId().equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    // Renamed for clarity: Checks if a slot conflicts with existing APPROVED bookings
    public static boolean isTimeSlotBooked(String roomNumber, LocalDate date, LocalTime proposedStartTime, LocalTime proposedEndTime) {
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
    // This is essentially the inverse of isTimeSlotBooked
    public static boolean isTimeSlotAvailable(String roomNumber, LocalDate date, LocalTime startTime, LocalTime endTime) {
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