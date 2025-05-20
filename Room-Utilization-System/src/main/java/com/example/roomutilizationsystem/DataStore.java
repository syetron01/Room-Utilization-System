
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
// Removed java.time.temporal.ChronoUnit as it's not directly used
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// --- Enums ---
enum UserRole { ADMIN, FACULTY, TEACHER, STAFF }
        enum BookingStatus { PENDING, APPROVED, REJECTED, CANCELLED }
        enum ScheduleRequestStatus {
            PENDING_APPROVAL,
            APPROVED,
            REJECTED
        }

// --- Data Classes ---
// Note: It's generally better practice for these to be top-level classes
// in their own .java files, but I'm keeping them as inner classes
// to match your existing structure and address the specific errors.

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

        class RoomSchedule {
            private final String scheduleId;
            private String roomNumber;
            private String roomType;
            private LocalTime startTime;
            private LocalTime endTime;
            private Set<DayOfWeek> daysOfWeek;
            private LocalDate definitionStartDate;
            private LocalDate definitionEndDate;
            private boolean isActive;

            static final AtomicLong idCounter = new AtomicLong(0); // Static counter for RoomSchedule
            private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            public RoomSchedule(String roomNumber, String roomType, LocalTime startTime, LocalTime endTime,
                                Set<DayOfWeek> daysOfWeek,
                                LocalDate definitionStartDate, LocalDate definitionEndDate) {
                this.scheduleId = "SCH-" + RoomSchedule.idCounter.incrementAndGet(); // Use class-specific counter
                this.roomNumber = Objects.requireNonNull(roomNumber, "Room number cannot be null").trim();
                this.roomType = Objects.requireNonNull(roomType, "Room type cannot be null");
                this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
                this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
                this.daysOfWeek = (daysOfWeek == null || daysOfWeek.isEmpty())
                        ? EnumSet.noneOf(DayOfWeek.class)
                        : EnumSet.copyOf(daysOfWeek);
                this.definitionStartDate = Objects.requireNonNull(definitionStartDate, "Definition start date cannot be null");
                this.definitionEndDate = Objects.requireNonNull(definitionEndDate, "Definition end date cannot be null");
                this.isActive = true;

                if (!this.daysOfWeek.isEmpty() && definitionStartDate.isAfter(definitionEndDate)) {
                    throw new IllegalArgumentException("Definition End Date cannot be before Start Date.");
                }
                if (startTime.equals(endTime) || startTime.isAfter(endTime)) {
                    throw new IllegalArgumentException("End time must be after start time.");
                }
                if (this.daysOfWeek.isEmpty()) {
                    System.err.println("Warning: Creating RoomSchedule definition with no days selected for ID " + scheduleId);
                }
            }

            public RoomSchedule(String scheduleId, String roomNumber, String roomType, LocalTime startTime, LocalTime endTime,
                                Set<DayOfWeek> daysOfWeek,
                                LocalDate definitionStartDate, LocalDate definitionEndDate, boolean isActive) {
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
                this.isActive = isActive;

                if (!this.daysOfWeek.isEmpty() && definitionStartDate.isAfter(definitionEndDate)) {
                    System.err.println("Warning: Loaded schedule definition "+scheduleId+" has end date before start date.");
                }
                if (startTime.equals(endTime) || startTime.isAfter(endTime)) {
                    System.err.println("Warning: Loaded schedule definition "+scheduleId+" has end time not after start time.");
                }
                if (this.daysOfWeek.isEmpty()) {
                    System.err.println("Warning: Loaded schedule definition "+scheduleId+" has no days of week.");
                }

                try {
                    String idNumStr = scheduleId.replace("SCH-", "");
                    if (!idNumStr.isEmpty()) {
                        long idNum = Long.parseLong(idNumStr);
                        RoomSchedule.idCounter.updateAndGet(current -> Math.max(current, idNum)); // Use class-specific counter
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse schedule ID: " + scheduleId + " for counter update.");
                }
            }

            public String getScheduleId() { return scheduleId; }
            public String getRoomNumber() { return roomNumber; }
            public String getRoomType() { return roomType; }
            public LocalTime getStartTime() { return startTime; }
            public LocalTime getEndTime() { return endTime; }
            public Set<DayOfWeek> getDaysOfWeek() { return Collections.unmodifiableSet(daysOfWeek); }
            public LocalDate getDefinitionStartDate() { return definitionStartDate; }
            public LocalDate getDefinitionEndDate() { return definitionEndDate; }
            public boolean isActive() { return isActive; }
            public void setActive(boolean active) { this.isActive = active; }
            public String getTimeRangeString() { return startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter); }
            public String getTimeColDisplay() { return getTimeRangeString(); }
            public String getDaysOfWeekDisplay() {
                if (daysOfWeek == null || daysOfWeek.isEmpty()) return "N/A";
                return daysOfWeek.stream()
                        .sorted(Comparator.naturalOrder())
                        .map(day -> day.toString().substring(0, 3))
                        .map(dayStr -> dayStr.charAt(0) + dayStr.substring(1).toLowerCase())
                        .collect(Collectors.joining(", "));
            }
            public String getDefinitionTypeDisplay() {
                if (daysOfWeek == null) return "Invalid";
                boolean spansMultipleDaysOrDates = daysOfWeek.size() > 1 || !definitionStartDate.equals(definitionEndDate);
                return spansMultipleDaysOrDates ? "Recurring" : "One Time";
            }
            public String getDefinitionEndDateDisplay() { return (definitionEndDate != null) ? definitionEndDate.format(dateFormatter) : "N/A"; }
            public String getDefinitionStartDateDisplay() { return (definitionStartDate != null) ? definitionStartDate.format(dateFormatter) : "N/A"; }
            public String getStatusDisplay() { return isActive ? "Active" : "Inactive"; }
            @Override public String toString() { return "ScheduleDef{id='" + scheduleId + "', room='" + roomNumber + "', days=" + getDaysOfWeekDisplay() + ", time=" + getTimeRangeString() + ", range=" + getDefinitionStartDateDisplay() + " to " + getDefinitionEndDateDisplay() + ", status=" + getStatusDisplay() + '}'; }
            @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; RoomSchedule that = (RoomSchedule) o; return Objects.equals(scheduleId, that.scheduleId); }
            @Override public int hashCode() { return Objects.hash(scheduleId); }
            public boolean covers(LocalDate date, LocalTime time) {
                if (!isActive) return false;
                if (date == null || time == null || daysOfWeek == null || daysOfWeek.isEmpty()) return false;
                boolean dateInRange = !date.isBefore(definitionStartDate) && !date.isAfter(definitionEndDate);
                if (!dateInRange) return false;
                boolean dayMatches = daysOfWeek.contains(date.getDayOfWeek());
                if (!dayMatches) return false;
                return !time.isBefore(startTime) && time.isBefore(endTime);
            }
            public boolean appliesOnDate(LocalDate date) {
                if (!isActive) return false;
                if (date == null || daysOfWeek == null || daysOfWeek.isEmpty()) return false;
                boolean dateInRange = !date.isBefore(definitionStartDate) && !date.isAfter(definitionEndDate);
                if (!dateInRange) return false;
                return daysOfWeek.contains(date.getDayOfWeek());
            }
        }

        class Booking {
            private final String bookingId;
            private String facultyUsername;
            private String roomNumber;
            private String roomType;
            private LocalDate date;
            private LocalTime bookedStartTime;
            private LocalTime bookedEndTime;
            private BookingStatus status;
            static final AtomicLong idCounter = new AtomicLong(0); // Static counter for Booking

            public Booking(User bookerUser, RoomSchedule roomSchedule, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime) {
                this.bookingId = "BOK-" + Booking.idCounter.incrementAndGet(); // Use class-specific counter
                if (bookerUser == null || bookerUser.getRole() == UserRole.ADMIN) { throw new IllegalArgumentException("Invalid user role for booking."); }
                if (roomSchedule == null || date == null || bookedStartTime == null || bookedEndTime == null) { throw new IllegalArgumentException("Booking details cannot be null."); }
                if (!bookedEndTime.isAfter(bookedStartTime)) { throw new IllegalArgumentException("Booking end time must be after start time."); }
                this.facultyUsername = bookerUser.getUsername();
                this.roomNumber = roomSchedule.getRoomNumber();
                this.roomType = roomSchedule.getRoomType();
                this.date = date;
                this.bookedStartTime = bookedStartTime;
                this.bookedEndTime = bookedEndTime;
                this.status = BookingStatus.PENDING;
            }
            public Booking(User bookerUser, String roomNumber, String roomType, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime) {
                this.bookingId = "BOK-" + Booking.idCounter.incrementAndGet(); // Use class-specific counter
                if (bookerUser == null || bookerUser.getRole() == UserRole.ADMIN) { throw new IllegalArgumentException("Invalid user or role for booking."); }
                if (roomNumber == null || roomNumber.trim().isEmpty() || roomType == null || roomType.trim().isEmpty() || date == null || bookedStartTime == null || bookedEndTime == null) { throw new IllegalArgumentException("Room details, date, and times cannot be null or empty for a custom booking."); }
                if (!bookedEndTime.isAfter(bookedStartTime)) { throw new IllegalArgumentException("Booking end time must be after start time."); }
                this.facultyUsername = bookerUser.getUsername();
                this.roomNumber = roomNumber.trim();
                this.roomType = roomType.trim();
                this.date = date;
                this.bookedStartTime = bookedStartTime;
                this.bookedEndTime = bookedEndTime;
                this.status = BookingStatus.PENDING;
            }
            public Booking(String bookingId, String facultyUsername, String roomNumber, String roomType, LocalDate date, LocalTime bookedStartTime, LocalTime bookedEndTime, BookingStatus status) {
                this.bookingId = bookingId;
                this.facultyUsername = facultyUsername;
                this.roomNumber = roomNumber;
                this.roomType = roomType;
                this.date = date;
                this.bookedStartTime = bookedStartTime;
                this.bookedEndTime = bookedEndTime;
                this.status = status;
                try { String idNumStr = bookingId.replace("BOK-", ""); if (!idNumStr.isEmpty()) { long idNum = Long.parseLong(idNumStr); Booking.idCounter.updateAndGet(current -> Math.max(current, idNum)); } } // Use class-specific counter
                catch (NumberFormatException e) { System.err.println("Could not parse booking ID: " + bookingId + " for counter update."); }
            }
            public String getBookingId() { return bookingId; }
            public User getBookerUser() { return DataStore.getUserByUsername(facultyUsername); } // DataStore is the outer class
            public String getFacultyUsername() { return facultyUsername; }
            public String getRoomNumber() { return roomNumber; }
            public String getRoomType() { return roomType; }
            public LocalDate getDate() { return date; }
            public LocalTime getBookedStartTime() { return bookedStartTime; }
            public LocalTime getBookedEndTime() { return bookedEndTime; }
            public BookingStatus getStatus() { return status; }
            public void setStatus(BookingStatus status) { this.status = Objects.requireNonNull(status); DataStore.saveBookingsOnly(); } // DataStore is outer class
            public String getFacultyRole() { User user = getBookerUser(); return user != null ? user.getRole().toString() : "UNKNOWN"; }
            public String getTimeScheduleDisplay() { DateTimeFormatter f = DateTimeFormatter.ofPattern("h:mm a"); return bookedStartTime.format(f) + " - " + bookedEndTime.format(f); }
            public String getBookedTimeRangeString() { return getTimeScheduleDisplay(); }
            public String getDayDisplay() { DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE; String fd = date.format(df); String dow = date.getDayOfWeek().toString(); dow = dow.substring(0, 1).toUpperCase() + dow.substring(1).toLowerCase(); return fd + " (" + dow + ")"; }
            public String getStatusDisplay() { return status.toString(); }
            @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Booking b = (Booking) o; return Objects.equals(bookingId, b.bookingId); }
            @Override public int hashCode() { return Objects.hash(bookingId); }
            @Override public String toString() { return "Booking{id='" + bookingId + "', user='" + facultyUsername + "', room='" + roomNumber + "', type='" + roomType + "', date=" + date + ", time=" + getTimeScheduleDisplay() + ", status=" + status + '}'; }
        }

        // This class is now an inner class of DataStore, like the others
        class ScheduleRequest {
            private final String requestId;
            private String requestedByUsername;
            private String roomNumber;
            private String roomType;
            private LocalTime startTime;
            private LocalTime endTime;
            private Set<DayOfWeek> daysOfWeek;
            private LocalDate definitionStartDate;
            private LocalDate definitionEndDate;
            private ScheduleRequestStatus status;

            static final AtomicLong idCounter = new AtomicLong(0); // Static counter for ScheduleRequest
            private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            public ScheduleRequest(String requestedByUsername, String roomNumber, String roomType,
                                   LocalTime startTime, LocalTime endTime, Set<DayOfWeek> daysOfWeek,
                                   LocalDate definitionStartDate, LocalDate definitionEndDate) {
                this.requestId = "SR-" + ScheduleRequest.idCounter.incrementAndGet(); // Use class-specific counter
                this.requestedByUsername = Objects.requireNonNull(requestedByUsername, "Requester username cannot be null");
                this.roomNumber = Objects.requireNonNull(roomNumber, "Room number cannot be null").trim();
                this.roomType = Objects.requireNonNull(roomType, "Room type cannot be null");
                this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
                this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
                this.daysOfWeek = (daysOfWeek == null || daysOfWeek.isEmpty()) ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(daysOfWeek);
                this.definitionStartDate = Objects.requireNonNull(definitionStartDate, "Definition start date cannot be null");
                this.definitionEndDate = Objects.requireNonNull(definitionEndDate, "Definition end date cannot be null");
                this.status = ScheduleRequestStatus.PENDING_APPROVAL;
                if (!this.daysOfWeek.isEmpty() && definitionStartDate.isAfter(definitionEndDate)) { throw new IllegalArgumentException("Definition End Date cannot be before Start Date for schedule request."); }
                if (startTime.equals(endTime) || startTime.isAfter(endTime)) { throw new IllegalArgumentException("End time must be after start time for schedule request."); }
            }
            public ScheduleRequest(String requestId, String requestedByUsername, String roomNumber, String roomType,
                                   LocalTime startTime, LocalTime endTime, Set<DayOfWeek> daysOfWeek,
                                   LocalDate definitionStartDate, LocalDate definitionEndDate, ScheduleRequestStatus status) {
                this.requestId = Objects.requireNonNull(requestId, "Request ID cannot be null");
                this.requestedByUsername = Objects.requireNonNull(requestedByUsername);
                this.roomNumber = Objects.requireNonNull(roomNumber).trim();
                this.roomType = Objects.requireNonNull(roomType);
                this.startTime = Objects.requireNonNull(startTime);
                this.endTime = Objects.requireNonNull(endTime);
                this.daysOfWeek = (daysOfWeek == null || daysOfWeek.isEmpty()) ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(daysOfWeek);
                this.definitionStartDate = Objects.requireNonNull(definitionStartDate);
                this.definitionEndDate = Objects.requireNonNull(definitionEndDate);
                this.status = Objects.requireNonNull(status);
                try { String idNumStr = requestId.replace("SR-", ""); if (!idNumStr.isEmpty()) { long idNum = Long.parseLong(idNumStr); ScheduleRequest.idCounter.updateAndGet(current -> Math.max(current, idNum)); } } // Use class-specific counter
                catch (NumberFormatException e) { System.err.println("Could not parse schedule request ID: " + requestId + " for counter update."); }
            }
            public String getRequestId() { return requestId; }
            public String getRequestedByUsername() { return requestedByUsername; }
            public User getRequesterUser() { return DataStore.getUserByUsername(requestedByUsername); } // DataStore is the outer class
            public String getRoomNumber() { return roomNumber; }
            public String getRoomType() { return roomType; }
            public LocalTime getStartTime() { return startTime; }
            public LocalTime getEndTime() { return endTime; }
            public Set<DayOfWeek> getDaysOfWeek() { return Collections.unmodifiableSet(daysOfWeek); }
            public LocalDate getDefinitionStartDate() { return definitionStartDate; }
            public LocalDate getDefinitionEndDate() { return definitionEndDate; }
            public ScheduleRequestStatus getStatus() { return status; }
            public void setStatus(ScheduleRequestStatus status) { this.status = Objects.requireNonNull(status); DataStore.saveScheduleRequestsOnly(); } // DataStore is outer class
            public String getTimeRangeString() { return startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter); }
            public String getDaysOfWeekDisplay() { if (daysOfWeek == null || daysOfWeek.isEmpty()) return "N/A"; return daysOfWeek.stream().sorted(Comparator.naturalOrder()).map(day -> day.toString().substring(0, 1).toUpperCase() + day.toString().substring(1, 3).toLowerCase()).collect(Collectors.joining(", ")); }
            public String getDefinitionDateRangeDisplay() { return definitionStartDate.format(dateFormatter) + " to " + definitionEndDate.format(dateFormatter); }
            public String getStatusDisplay() { return status.toString().replace("_", " "); }
            @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; ScheduleRequest that = (ScheduleRequest) o; return Objects.equals(requestId, that.requestId); }
            @Override public int hashCode() { return Objects.hash(requestId); }
            @Override public String toString() { return "ScheduleRequest{id='" + requestId + "', user='" + requestedByUsername + "', room='" + roomNumber + "', days=" + getDaysOfWeekDisplay() + ", time=" + getTimeRangeString() + ", range=" + getDefinitionDateRangeDisplay() + ", status=" + status + '}'; }
        }


        public class DataStore {

            private static final Path DATA_DIR = Paths.get("data");
            private static final Path ROOMS_FILE = DATA_DIR.resolve("rooms.txt");
            private static final Path BOOKINGS_FILE = DATA_DIR.resolve("bookings.txt");
            private static final Path USERS_FILE = DATA_DIR.resolve("users.txt");
            private static final Path SCHEDULE_REQUESTS_FILE = DATA_DIR.resolve("schedule_requests.txt");

            static final Map<String, User> users = new HashMap<>();
            static final List<RoomSchedule> roomSchedules = new ArrayList<>();
            static final List<Booking> bookings = new ArrayList<>();
            static final List<ScheduleRequest> scheduleRequests = new ArrayList<>(); // Use unqualified ScheduleRequest

            static final DateTimeFormatter TIME_FILE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
            static final DateTimeFormatter DATE_FILE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
            static final String DAYS_OF_WEEK_SEPARATOR = ",";

            private static User currentLoggedInUser;

            static {
                ensureDataDirectoryExists();
                loadData();
                if (users.isEmpty()) {
                    System.out.println("No users loaded, adding default admin (admin/admin123)...");
                    addUser("admin", "admin123", UserRole.ADMIN);
                }
                // resetIdCounters is called within loadData after all data is loaded
            }

            private static void resetIdCounters() {
                long maxScheduleId = roomSchedules.stream()
                        .map(RoomSchedule::getScheduleId)
                        .map(id -> id.replace("SCH-", ""))
                        .filter(s -> !s.isEmpty()) // handle cases where replace results in empty string if original was just "SCH-"
                        .mapToLong(numStr -> { try { return Long.parseLong(numStr); } catch (NumberFormatException e) { return 0L; } })
                        .max().orElse(0L);
                RoomSchedule.idCounter.set(maxScheduleId);

                long maxBookingId = bookings.stream()
                        .map(Booking::getBookingId)
                        .map(id -> id.replace("BOK-", ""))
                        .filter(s -> !s.isEmpty())
                        .mapToLong(numStr -> { try { return Long.parseLong(numStr); } catch (NumberFormatException e) { return 0L; } })
                        .max().orElse(0L);
                Booking.idCounter.set(maxBookingId);

                long maxScheduleRequestId = scheduleRequests.stream()
                        .map(ScheduleRequest::getRequestId) // Correctly call on ScheduleRequest instance
                        .map(id -> { // Ensure 'id' is a String before calling replace
                            if (id instanceof String) {
                                return ((String) id).replace("SR-", "");
                            }
                            return ""; // Or handle error appropriately
                        })
                        .filter(s -> !s.isEmpty())
                        .mapToLong(numStr -> { try { return Long.parseLong(numStr); } catch (NumberFormatException e) { return 0L; } })
                        .max().orElse(0L);
                ScheduleRequest.idCounter.set(maxScheduleRequestId); // Access static counter of inner class ScheduleRequest

                System.out.println("Reset counters based on loaded data: Next Schedule ID will be SCH-" + (RoomSchedule.idCounter.get() + 1) +
                        ", Next Booking ID will be BOK-" + (Booking.idCounter.get() + 1) +
                        ", Next Schedule Request ID will be SR-" + (ScheduleRequest.idCounter.get() + 1));
            }


            private static void ensureDataDirectoryExists() {
                if (!Files.exists(DATA_DIR)) {
                    try {
                        Files.createDirectories(DATA_DIR);
                        System.out.println("Created data directory: " + DATA_DIR.toAbsolutePath());
                    } catch (IOException e) {
                        System.err.println("FATAL: Error creating data directory: " + DATA_DIR.toAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }

            private static synchronized void loadData() {
                System.out.println("Loading data from .txt files...");
                users.clear(); // Clear existing data before loading
                roomSchedules.clear();
                bookings.clear();
                scheduleRequests.clear();

                RoomSchedule.idCounter.set(0); // Reset counters before loading
                Booking.idCounter.set(0);
                ScheduleRequest.idCounter.set(0);


                loadUsers();
                loadRoomSchedules();
                loadBookings();
                loadScheduleRequests();
                System.out.println("Data loading complete.");
                resetIdCounters(); // Reset counters based on MAX loaded IDs
            }

            public static synchronized void saveData() {
                System.out.println("Saving all data...");
                saveUsers();
                saveRoomSchedules();
                saveBookings();
                saveScheduleRequests();
                System.out.println("All data saved.");
            }

            public static synchronized void saveScheduleRequestsOnly() {
                System.out.println("Saving schedule requests only...");
                saveScheduleRequests();
                System.out.println("Schedule requests saved.");
            }

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

            private static final String USER_HEADER = "username|password|role";
            private static void loadUsers() {
                // users.clear(); // Already cleared in loadData
                if (!Files.exists(USERS_FILE)) {
                    System.out.println("Users file not found: " + USERS_FILE.toAbsolutePath());
                    return;
                }
                try (BufferedReader reader = Files.newBufferedReader(USERS_FILE)) {
                    String header = reader.readLine();
                    if (header == null || !header.equals(USER_HEADER)) {
                        System.err.println("Warning: Invalid users file header in " + USERS_FILE.toAbsolutePath());
                        return;
                    }
                    String line;
                    int lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        String[] parts = line.split("\\|", -1);
                        if (parts.length == 3) {
                            try {
                                String username = parts[0].trim();
                                String password = parts[1];
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
                } catch (IOException e) {
                    System.err.println("Error writing users file: " + USERS_FILE.toAbsolutePath());
                    e.printStackTrace();
                }
            }

            private static final String SCHEDULE_HEADER = "scheduleId|roomNumber|roomType|startTime|endTime|daysOfWeek|definitionStartDate|definitionEndDate|isActive";
            private static void loadRoomSchedules() {
                // roomSchedules.clear(); // Already cleared in loadData
                // RoomSchedule.idCounter.set(0); // Already reset in loadData
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
                        if (parts.length == 9) {
                            try {
                                String scheduleId = parts[0].trim();
                                String roomNumber = parts[1].trim();
                                String roomType = parts[2].trim();
                                LocalTime startTime = LocalTime.parse(parts[3].trim(), TIME_FILE_FORMATTER);
                                LocalTime endTime = LocalTime.parse(parts[4].trim(), TIME_FILE_FORMATTER);
                                Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
                                String daysStr = parts[5].trim();
                                if (!daysStr.isEmpty()) {
                                    String[] dayNames = daysStr.split(DAYS_OF_WEEK_SEPARATOR);
                                    for (String dayName : dayNames) {
                                        try {
                                            if (!dayName.trim().isEmpty()) { daysOfWeek.add(DayOfWeek.valueOf(dayName.trim().toUpperCase())); }
                                        } catch (IllegalArgumentException e) { System.err.println("Skipping invalid DayOfWeek '" + dayName + "' in schedule line " + lineNum + ": " + line); }
                                    }
                                }
                                LocalDate defStartDate = LocalDate.parse(parts[6].trim(), DATE_FILE_FORMATTER);
                                LocalDate defEndDate = LocalDate.parse(parts[7].trim(), DATE_FILE_FORMATTER);
                                boolean isActive = Boolean.parseBoolean(parts[8].trim());
                                RoomSchedule schedule = new RoomSchedule(scheduleId, roomNumber, roomType, startTime, endTime, daysOfWeek, defStartDate, defEndDate, isActive);
                                roomSchedules.add(schedule);
                            } catch (DateTimeParseException e) { System.err.println("Skipping invalid schedule definition line "+lineNum+" (parse error): " + line + " - " + e.getMessage()); }
                            catch (IllegalArgumentException e) { System.err.println("Skipping invalid schedule definition line "+lineNum+" (value error): " + line + " - " + e.getMessage()); }
                            catch (Exception e) { System.err.println("Skipping invalid schedule definition line "+lineNum+" (unexpected error): " + line + " - " + e.getMessage()); e.printStackTrace(); }
                        } else { System.err.println("Skipping invalid schedule definition line "+lineNum+" (wrong field count, expected 9): " + line); }
                    }
                    System.out.println("Loaded " + roomSchedules.size() + " room schedule definitions from " + ROOMS_FILE.toAbsolutePath());
                } catch (IOException e) { System.err.println("Error reading room schedules file: " + ROOMS_FILE.toAbsolutePath()); e.printStackTrace(); }
            }

            private static void saveRoomSchedules() {
                try (BufferedWriter writer = Files.newBufferedWriter(ROOMS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(SCHEDULE_HEADER);
                    writer.newLine();
                    for (RoomSchedule schedule : roomSchedules) {
                        String daysOfWeekStr = schedule.getDaysOfWeek().stream().sorted().map(Enum::name).collect(Collectors.joining(DAYS_OF_WEEK_SEPARATOR));
                        String defStartDateStr = schedule.getDefinitionStartDate().format(DATE_FILE_FORMATTER);
                        String defEndDateStr = schedule.getDefinitionEndDate().format(DATE_FILE_FORMATTER);
                        writer.write(schedule.getScheduleId() + "|" + schedule.getRoomNumber() + "|" + schedule.getRoomType() + "|" +
                                schedule.getStartTime().format(TIME_FILE_FORMATTER) + "|" + schedule.getEndTime().format(TIME_FILE_FORMATTER) + "|" +
                                daysOfWeekStr + "|" + defStartDateStr + "|" + defEndDateStr + "|" + schedule.isActive());
                        writer.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Error writing room schedules file: " + ROOMS_FILE.toAbsolutePath());
                    e.printStackTrace();
                }
            }

            private static final String BOOKING_HEADER = "bookingId|facultyUsername|roomNumber|roomType|date|bookedStartTime|bookedEndTime|status";
            private static void loadBookings() {
                // bookings.clear(); // Already cleared in loadData
                // Booking.idCounter.set(0); // Already reset in loadData
                if (!Files.exists(BOOKINGS_FILE)) { System.out.println("Bookings file not found: " + BOOKINGS_FILE.toAbsolutePath()); return; }
                if (TIME_FILE_FORMATTER == null || DATE_FILE_FORMATTER == null) { System.err.println("FATAL: Formatters null in loadBookings"); return; }

                try (BufferedReader reader = Files.newBufferedReader(BOOKINGS_FILE)) {
                    String header = reader.readLine();
                    if (header == null || !header.equals(BOOKING_HEADER)) { System.err.println("Warning: Invalid bookings file header in " + BOOKINGS_FILE.toAbsolutePath()); return; }
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
                                } else { System.err.println("Skipping booking on line " + lineNum + " for non-existent user '" + username + "': " + line); }
                            } catch (IllegalArgumentException | DateTimeParseException e) { System.err.println("Skipping invalid booking line "+lineNum+" (parse error): " + line + " - " + e.getMessage()); }
                            catch (Exception e) { System.err.println("Skipping invalid booking line "+lineNum+" (unexpected error): " + line + " - " + e.getMessage()); e.printStackTrace(); }
                        } else { System.err.println("Skipping invalid booking line "+lineNum+" (wrong field count, expected 8): " + line); }
                    }
                    System.out.println("Loaded " + bookings.size() + " bookings from " + BOOKINGS_FILE.toAbsolutePath());
                } catch (IOException e) { System.err.println("Error reading bookings file: " + BOOKINGS_FILE.toAbsolutePath()); e.printStackTrace(); }
            }

            static void saveBookings() { // Package-private for Booking class to call
                try (BufferedWriter writer = Files.newBufferedWriter(BOOKINGS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(BOOKING_HEADER);
                    writer.newLine();
                    for (Booking booking : bookings) {
                        writer.write(booking.getBookingId() + "|" + booking.getFacultyUsername() + "|" + booking.getRoomNumber() + "|" +
                                booking.getRoomType() + "|" + booking.getDate().format(DATE_FILE_FORMATTER) + "|" +
                                booking.getBookedStartTime().format(TIME_FILE_FORMATTER) + "|" + booking.getBookedEndTime().format(TIME_FILE_FORMATTER) + "|" +
                                booking.getStatus().name());
                        writer.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Error writing bookings file: " + BOOKINGS_FILE.toAbsolutePath());
                    e.printStackTrace();
                }
            }

            private static final String SCHEDULE_REQUEST_HEADER = "requestId|requestedByUsername|roomNumber|roomType|startTime|endTime|daysOfWeek|definitionStartDate|definitionEndDate|status";
            private static void loadScheduleRequests() {
                // scheduleRequests.clear(); // Already cleared in loadData
                // ScheduleRequest.idCounter.set(0); // Already reset in loadData
                if (!Files.exists(SCHEDULE_REQUESTS_FILE)) { System.out.println("Schedule requests file not found: " + SCHEDULE_REQUESTS_FILE.toAbsolutePath() + ". No requests loaded."); return; }
                try (BufferedReader reader = Files.newBufferedReader(SCHEDULE_REQUESTS_FILE)) {
                    String header = reader.readLine();
                    if (header == null || !header.equals(SCHEDULE_REQUEST_HEADER)) { System.err.println("Warning: Invalid schedule requests file header in " + SCHEDULE_REQUESTS_FILE.toAbsolutePath() + ". Skipping load."); return; }
                    String line;
                    int lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        String[] parts = line.split("\\|", -1);
                        if (parts.length == 10) {
                            try {
                                String reqId = parts[0].trim();
                                String username = parts[1].trim();
                                String roomNum = parts[2].trim();
                                String roomType = parts[3].trim();
                                LocalTime startTime = LocalTime.parse(parts[4].trim(), TIME_FILE_FORMATTER);
                                LocalTime endTime = LocalTime.parse(parts[5].trim(), TIME_FILE_FORMATTER);
                                Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
                                String daysStr = parts[6].trim();
                                if (!daysStr.isEmpty()) { for (String dayName : daysStr.split(DAYS_OF_WEEK_SEPARATOR)) { if (!dayName.trim().isEmpty()) days.add(DayOfWeek.valueOf(dayName.trim().toUpperCase())); } }
                                LocalDate defStartDate = LocalDate.parse(parts[7].trim(), DATE_FILE_FORMATTER);
                                LocalDate defEndDate = LocalDate.parse(parts[8].trim(), DATE_FILE_FORMATTER);
                                ScheduleRequestStatus status = ScheduleRequestStatus.valueOf(parts[9].trim().toUpperCase());
                                ScheduleRequest request = new ScheduleRequest(reqId, username, roomNum, roomType, startTime, endTime, days, defStartDate, defEndDate, status);
                                scheduleRequests.add(request);
                            } catch (Exception e) { System.err.println("Skipping invalid schedule request line " + lineNum + ": " + line + " - " + e.getMessage()); }
                        } else { System.err.println("Skipping invalid schedule request line " + lineNum + " (wrong field count, expected 10): " + line); }
                    }
                    System.out.println("Loaded " + scheduleRequests.size() + " schedule requests from " + SCHEDULE_REQUESTS_FILE.toAbsolutePath());
                } catch (IOException e) { System.err.println("Error reading schedule requests file: " + SCHEDULE_REQUESTS_FILE.toAbsolutePath()); e.printStackTrace(); }
            }

            private static void saveScheduleRequests() {
                try (BufferedWriter writer = Files.newBufferedWriter(SCHEDULE_REQUESTS_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(SCHEDULE_REQUEST_HEADER);
                    writer.newLine();
                    for (ScheduleRequest request : scheduleRequests) {
                        String daysOfWeekStr = request.getDaysOfWeek().stream().sorted().map(Enum::name).collect(Collectors.joining(DAYS_OF_WEEK_SEPARATOR));
                        writer.write(request.getRequestId() + "|" + request.getRequestedByUsername() + "|" + request.getRoomNumber() + "|" +
                                request.getRoomType() + "|" + request.getStartTime().format(TIME_FILE_FORMATTER) + "|" + request.getEndTime().format(TIME_FILE_FORMATTER) + "|" +
                                daysOfWeekStr + "|" + request.getDefinitionStartDate().format(DATE_FILE_FORMATTER) + "|" + request.getDefinitionEndDate().format(DATE_FILE_FORMATTER) + "|" +
                                request.getStatus().name());
                        writer.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Error writing schedule requests file: " + SCHEDULE_REQUESTS_FILE.toAbsolutePath());
                    e.printStackTrace();
                }
            }

            public static User authenticateUser(String u, String p) { User user = users.get(u); if (user != null && user.getPassword().equals(p)) { setLoggedInUser(user); return user; } setLoggedInUser(null); return null; }
            public static User getUserByUsername(String u) { return users.get(u); }
            public static boolean containsUser(String u) { return users.containsKey(u); }
            public static synchronized boolean addUser(String u, String p, UserRole r) { if (u == null || u.trim().isEmpty() || p == null || p.isEmpty() || r == null) { System.err.println("Error adding user: Username, password, and role cannot be empty."); return false; } String trimmedUsername = u.trim(); if (users.containsKey(trimmedUsername)) { System.err.println("Error adding user: Username '" + trimmedUsername + "' already exists."); return false; } users.put(trimmedUsername, new User(trimmedUsername, p, r)); saveUsersOnly(); System.out.println("User added: " + trimmedUsername + " Role: " + r); return true; }
            public static void setLoggedInUser(User u) { currentLoggedInUser = u; System.out.println("User logged in: " + (u != null ? u.getUsername() + " [" + u.getRole() + "]" : "null")); }
            public static User getLoggedInUser() { return currentLoggedInUser; }
            public static void logout() { System.out.println("User logged out: " + (currentLoggedInUser != null ? currentLoggedInUser.getUsername() : "null")); currentLoggedInUser = null; }

            public static synchronized boolean addRoomScheduleDefinition(RoomSchedule definitionToAdd) {
                if (definitionToAdd == null || definitionToAdd.getDaysOfWeek() == null) { System.err.println("Cannot add null schedule definition or one without daysOfWeek."); return false; }
                if (definitionToAdd.getDaysOfWeek().isEmpty()) { System.err.println("Warning: Adding schedule definition " + definitionToAdd.getScheduleId() + " with no days selected."); }
                boolean conflict = roomSchedules.stream().filter(existingDef -> existingDef.getRoomNumber().equalsIgnoreCase(definitionToAdd.getRoomNumber())).anyMatch(existingDef -> { boolean datesOverlap = definitionToAdd.getDefinitionStartDate().isBefore(existingDef.getDefinitionEndDate().plusDays(1)) && definitionToAdd.getDefinitionEndDate().isAfter(existingDef.getDefinitionStartDate().minusDays(1)); if (!datesOverlap) return false; boolean timesOverlap = definitionToAdd.getStartTime().isBefore(existingDef.getEndTime()) && definitionToAdd.getEndTime().isAfter(existingDef.getStartTime()); if (!timesOverlap) return false; return !Collections.disjoint(definitionToAdd.getDaysOfWeek(), existingDef.getDaysOfWeek()); });
                if (conflict) { System.out.println("Schedule Definition conflict detected for Room " + definitionToAdd.getRoomNumber() + " for days " + definitionToAdd.getDaysOfWeekDisplay() + " between " + definitionToAdd.getTimeRangeString() + " within date range " + definitionToAdd.getDefinitionStartDateDisplay() + " to " + definitionToAdd.getDefinitionEndDateDisplay()); List<RoomSchedule> conflicts = roomSchedules.stream().filter(existingDef -> existingDef.getRoomNumber().equalsIgnoreCase(definitionToAdd.getRoomNumber())).filter(existingDef -> { boolean datesOverlap = definitionToAdd.getDefinitionStartDate().isBefore(existingDef.getDefinitionEndDate().plusDays(1)) && definitionToAdd.getDefinitionEndDate().isAfter(existingDef.getDefinitionStartDate().minusDays(1)); if (!datesOverlap) return false; boolean timesOverlap = definitionToAdd.getStartTime().isBefore(existingDef.getEndTime()) && definitionToAdd.getEndTime().isAfter(existingDef.getStartTime()); if (!timesOverlap) return false; return !Collections.disjoint(definitionToAdd.getDaysOfWeek(), existingDef.getDaysOfWeek()); }).collect(Collectors.toList()); System.out.println("Conflicts with existing definition(s): " + conflicts.stream().map(RoomSchedule::getScheduleId).collect(Collectors.joining(", "))); return false; }
                roomSchedules.add(definitionToAdd); System.out.println("Schedule Definition added: " + definitionToAdd); saveRoomSchedulesOnly(); return true;
            }
            public static List<RoomSchedule> getAllRoomSchedules() { return new ArrayList<>(roomSchedules); }
            public static synchronized boolean removeRoomSchedule(String id) { boolean removed = roomSchedules.removeIf(s -> s.getScheduleId().equals(id)); if(removed) { System.out.println("Schedule definition removed: " + id); saveRoomSchedulesOnly(); } else { System.out.println("Remove schedule definition failed: ID " + id + " not found."); } return removed; }
            public static RoomSchedule findScheduleById(String id) { if (id == null) return null; return roomSchedules.stream().filter(s -> id.equals(s.getScheduleId())).findFirst().orElse(null); }
            public static Optional<RoomSchedule> findCoveringScheduleDefinition(String roomNumber, LocalDate date, LocalTime time) { if (roomNumber == null || date == null || time == null) return Optional.empty(); return roomSchedules.stream().filter(RoomSchedule::isActive).filter(def -> def.getRoomNumber().equalsIgnoreCase(roomNumber)).filter(def -> def.appliesOnDate(date)).filter(def -> !time.isBefore(def.getStartTime()) && time.isBefore(def.getEndTime())).findFirst(); }
            public static synchronized boolean addBooking(Booking bookingToAdd) { if (bookingToAdd == null) { System.err.println("Attempted to add a null booking."); return false; } if (isTimeSlotBooked(bookingToAdd.getRoomNumber(), bookingToAdd.getDate(), bookingToAdd.getBookedStartTime(), bookingToAdd.getBookedEndTime())) { System.err.println("Booking Conflict: Room " + bookingToAdd.getRoomNumber() + " on " + bookingToAdd.getDate() + " from " + bookingToAdd.getBookedStartTime() + " to " + bookingToAdd.getBookedEndTime() + " is already booked (Approved)."); return false; } Duration duration = Duration.between(bookingToAdd.getBookedStartTime(), bookingToAdd.getBookedEndTime()); if (duration.toMinutes() < 15 || duration.toMinutes() > 180) { System.err.println("Booking Invalid Duration for " + bookingToAdd.getBookingId() + ": " + duration.toMinutes() + " minutes. Must be between 15 and 180."); return false; } Optional<RoomSchedule> coveringDef = findCoveringScheduleDefinition(bookingToAdd.getRoomNumber(), bookingToAdd.getDate(), bookingToAdd.getBookedStartTime()); if (coveringDef.isEmpty() || !bookingToAdd.getBookedEndTime().isAfter(bookingToAdd.getBookedStartTime()) || bookingToAdd.getBookedEndTime().isAfter(coveringDef.get().getEndTime())) { System.err.println("Booking Slot No Longer Available: " + bookingToAdd.getBookingId() + " at " + bookingToAdd.getDate() + " " + bookingToAdd.getBookedTimeRangeString() + " for room " + bookingToAdd.getRoomNumber() + " does not fall within a currently defined and valid availability schedule (Start: " + (coveringDef.map(RoomSchedule::getStartTime).map(Object::toString).orElse("N/A")) + ", End: " + (coveringDef.map(RoomSchedule::getEndTime).map(Object::toString).orElse("N/A")) + ")."); return false; } bookings.add(bookingToAdd); System.out.println("Booking added (Pending): " + bookingToAdd); saveBookingsOnly(); return true; }
            public static synchronized boolean addScheduleRequest(ScheduleRequest requestToAdd) { if (requestToAdd == null) { System.err.println("Cannot add null schedule request."); return false; } scheduleRequests.add(requestToAdd); System.out.println("Schedule Request added: " + requestToAdd); saveScheduleRequestsOnly(); return true; }
            public static List<ScheduleRequest> getAllScheduleRequests() { return new ArrayList<>(scheduleRequests); }
            public static List<ScheduleRequest> getScheduleRequestsByUser(String username) { if (username == null) return new ArrayList<>(); return scheduleRequests.stream().filter(sr -> username.equals(sr.getRequestedByUsername())).collect(Collectors.toList()); }
            public static ScheduleRequest findScheduleRequestById(String id) { if (id == null) return null; return scheduleRequests.stream().filter(sr -> id.equals(sr.getRequestId())).findFirst().orElse(null); }
            public static List<Booking> getAllBookings() { return new ArrayList<>(bookings); }
            public static List<Booking> getBookingsByUser(User u) { if (u == null) return new ArrayList<>(); return bookings.stream().filter(b -> u.getUsername().equals(b.getFacultyUsername())).collect(Collectors.toList()); }
            public static List<Booking> getBookingsByStatus(BookingStatus s) { if (s == null) return new ArrayList<>(); return bookings.stream().filter(b -> b.getStatus() == s).collect(Collectors.toList()); }
            public static Booking findBookingById(String id) { if (id == null) return null; return bookings.stream().filter(b -> id.equals(b.getBookingId())).findFirst().orElse(null); }
            public static boolean isTimeSlotBooked(String roomNumber, LocalDate date, LocalTime requestedStartTime, LocalTime requestedEndTime) { if (roomNumber == null || date == null || requestedStartTime == null || requestedEndTime == null || !requestedEndTime.isAfter(requestedStartTime)) { System.err.println("Warning: Invalid input to isTimeSlotBooked."); return true; } return bookings.stream().filter(existingBooking -> existingBooking.getStatus() == BookingStatus.APPROVED).filter(existingBooking -> existingBooking.getRoomNumber().equalsIgnoreCase(roomNumber)).filter(existingBooking -> existingBooking.getDate().equals(date)).anyMatch(existingBooking -> requestedStartTime.isBefore(existingBooking.getBookedEndTime()) && requestedEndTime.isAfter(existingBooking.getBookedStartTime())); }
            public static boolean isTimeSlotAvailable(String roomNumber, LocalDate date, LocalTime startTime, LocalTime endTime) { return !isTimeSlotBooked(roomNumber, date, startTime, endTime); }
            public static long getTotalRoomSchedules() { return roomSchedules.stream().filter(RoomSchedule::isActive).count(); }
            public static long countSchedulesByType(String t) { if (t == null) return 0; return roomSchedules.stream().filter(RoomSchedule::isActive).filter(s -> t.equalsIgnoreCase(s.getRoomType())).count(); }
            public static List<String> getDistinctRoomNumbers() { return roomSchedules.stream().filter(RoomSchedule::isActive).map(RoomSchedule::getRoomNumber).distinct().sorted().collect(Collectors.toList()); }
            public static List<String> getDistinctRoomTypes() { return roomSchedules.stream().filter(RoomSchedule::isActive).map(RoomSchedule::getRoomType).filter(type -> "Laboratory".equalsIgnoreCase(type) || "Lecture".equalsIgnoreCase(type)).distinct().sorted().collect(Collectors.toList()); }
        }
