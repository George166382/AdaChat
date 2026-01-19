
        package com.example.andopsi.data;

import com.example.andopsi.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostgresDataSource {

    private static final Logger LOGGER = Logger.getLogger(PostgresDataSource.class.getName());

    // ============== Custom Exceptions ==============

    public static class DatabaseException extends Exception {
        public DatabaseException(String message) {
            super(message);
        }
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class UserNotFoundException extends DatabaseException {
        public UserNotFoundException(String username, String email) {
            super(String.format("User not found: username='%s', email='%s'", username, email));
        }
    }

    public static class UserAlreadyExistsException extends DatabaseException {
        public UserAlreadyExistsException(String username, String email) {
            super(String.format("User already exists: username='%s', email='%s'", username, email));
        }
    }

    public static class UserMetaNotFoundException extends DatabaseException {
        public UserMetaNotFoundException(String username, String email, String metaKey) {
            super(String.format("UserMeta not found: username='%s', email='%s', key='%s'", username, email, metaKey));
        }
    }

    public static class VideoProviderNotFoundException extends DatabaseException {
        public VideoProviderNotFoundException(String providerSlug) {
            super(String.format("Video provider not found: slug='%s'", providerSlug));
        }
    }

    public static class VideoProviderAlreadyExistsException extends DatabaseException {
        public VideoProviderAlreadyExistsException(String providerSlug) {
            super(String.format("Video provider already exists: slug='%s'", providerSlug));
        }
    }

    public static class VideoNotFoundException extends DatabaseException {
        public VideoNotFoundException(String providerSlug, String nativeProviderId) {
            super(String.format("Video not found: provider='%s', id='%s'", providerSlug, nativeProviderId));
        }
    }

    public static class VideoAlreadyExistsException extends DatabaseException {
        public VideoAlreadyExistsException(String providerSlug, String nativeProviderId) {
            super(String.format("Video already exists: provider='%s', id='%s'", providerSlug, nativeProviderId));
        }
    }

    public static class CategoryNotFoundException extends DatabaseException {
        public CategoryNotFoundException(String categoryName) {
            super(String.format("Category not found: name='%s'", categoryName));
        }
    }

    public static class CategoryAlreadyExistsException extends DatabaseException {
        public CategoryAlreadyExistsException(String categoryName) {
            super(String.format("Category already exists: name='%s'", categoryName));
        }
    }

    public static class TagNotFoundException extends DatabaseException {
        public TagNotFoundException(String tagName) {
            super(String.format("Tag not found: name='%s'", tagName));
        }
    }

    public static class TagAlreadyExistsException extends DatabaseException {
        public TagAlreadyExistsException(String tagName) {
            super(String.format("Tag already exists: name='%s'", tagName));
        }
    }

    public static class CaptionTrackNotFoundException extends DatabaseException {
        public CaptionTrackNotFoundException(String providerSlug, String nativeProviderId, String language) {
            super(String.format("Caption track not found: provider='%s', id='%s', language='%s'",
                    providerSlug, nativeProviderId, language));
        }
    }

    public static class TranscriptNotFoundException extends DatabaseException {
        public TranscriptNotFoundException(String providerSlug, String nativeProviderId, String language) {
            super(String.format("Transcript not found: provider='%s', id='%s', language='%s'",
                    providerSlug, nativeProviderId, language));
        }
    }

    public static class InvalidDataException extends DatabaseException {
        public InvalidDataException(String message) {
            super("Invalid data: " + message);
        }
    }

    // ============== Result Class ==============

    @FunctionalInterface
    interface ConnectionCallback<T> {
        T doInConnection(Connection conn) throws SQLException, DatabaseException;
    }

    public static class Result<T> {
        public final T data;
        public final boolean success;
        public final Exception error;

        private Result(T data, boolean success, Exception error) {
            this.data = data;
            this.success = success;
            this.error = error;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(data, true, null);
        }

        public static <T> Result<T> failure(Exception e) {
            return new Result<>(null, false, e);
        }
    }

    // ⚠️ Security Warning: Do not hardcode credentials in a real Android app.
    private final String url = "jdbc:postgresql://localhost:5432/Cucuteni";
    private final String user = "postgres";
    private final String password = "JiJiK2002";

    // ============== Helper Methods ==============



    private <T> Result<T> executeQuery(ConnectionCallback<T> callback) throws SQLException, DatabaseException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Postgres JDBC driver not found", e);
        }

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            T data = callback.doInConnection(connection);
            return Result.success(data);
        }
    }
    // The wrapper that catches the re-thrown SQLException
    private <T> Result<T> execute(ConnectionCallback<T> action) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            return Result.success(action.doInConnection(conn));
        } catch (Exception e) {
            // This catches the 'throw e' from syncUsers
            // Logs the error and returns a safe 'Failure' result to your Worker
            return Result.failure(e);
        }
    }
    // In PostgresDataSource.java

    // In com.example.andopsi.data.PostgresDataSource.java

    // In com.example.andopsi.data.PostgresDataSource.java

    // In com.example.andopsi.data.PostgresDataSource.java

    public Result<Void> syncUsers(List<User> users) {
        // FIX: Add "OVERRIDING SYSTEM VALUE" to force Postgres to accept the local ID
        String sql = "INSERT INTO users (user_id, user_name, user_email, passwd_hash, display_name, " +
                "avatar_url, role, status, is_verified, last_login_at, created_at, updated_at) " +
                "OVERRIDING SYSTEM VALUE " +  // <--- ADD THIS LINE
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (user_id) DO UPDATE SET " +
                "user_name = EXCLUDED.user_name, " +
                "user_email = EXCLUDED.user_email, " +
                "display_name = EXCLUDED.display_name, " +
                "updated_at = EXCLUDED.updated_at";

        return execute(conn -> {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (User user : users) {
                    stmt.setInt(1, user.getId());
                    stmt.setString(2, user.getUsername());
                    stmt.setString(3, user.getEmail());

                    // Keep the Byte fix from the previous step
                    if (user.getPasswordHash() != null) {
                        stmt.setBytes(4, user.getPasswordHash().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } else {
                        stmt.setNull(4, Types.BINARY);
                    }

                    stmt.setString(5, user.getDisplayName());
                    stmt.setString(6, user.getAvatarUrl());
                    stmt.setString(7, user.getRole().name());
                    stmt.setString(8, user.getStatus().name());
                    stmt.setBoolean(9, user.isVerified());

                    if (user.getLastLoginAt() != null) {
                        stmt.setTimestamp(10, new Timestamp(user.getLastLoginAt().toEpochMilliseconds()));
                    } else {
                        stmt.setNull(10, Types.TIMESTAMP);
                    }

                    stmt.setTimestamp(11, new Timestamp(user.getCreatedAt().toEpochMilliseconds()));
                    stmt.setTimestamp(12, new Timestamp(user.getUpdatedAt().toEpochMilliseconds()));

                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
            return null;
        });
    }





    /*private boolean userExists(Connection conn, String username, String email) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM users WHERE username = ? AND email = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean userMetaExists(Connection conn, String username, String email, String metaKey) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM usermeta um " +
                "JOIN users u ON um.user_id = u.user_id " +
                "WHERE u.username = ? AND u.email = ? AND um.meta_key = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, metaKey);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean videoProviderExists(Connection conn, String providerSlug) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM video_providers WHERE provider_slug = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, providerSlug);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean videoExists(Connection conn, String providerSlug, String nativeProviderId) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM videos v " +
                "JOIN video_providers vp ON v.provider_id = vp.provider_id " +
                "WHERE vp.provider_slug = ? AND v.native_provider_id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, providerSlug);
            stmt.setString(2, nativeProviderId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean categoryExists(Connection conn, String categoryName) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM categories WHERE category_name = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, categoryName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean tagExists(Connection conn, String tagName) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM tags WHERE tag_name = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tagName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean captionTrackExists(Connection conn, String providerSlug, String nativeProviderId, String language) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM caption_tracks ct " +
                "JOIN videos v ON ct.video_id = v.video_id " +
                "JOIN video_providers vp ON v.provider_id = vp.provider_id " +
                "WHERE vp.provider_slug = ? AND v.native_provider_id = ? AND ct.language = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, providerSlug);
            stmt.setString(2, nativeProviderId);
            stmt.setString(3, language);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean transcriptExists(Connection conn, String providerSlug, String nativeProviderId, String language) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM transcripts t " +
                "JOIN videos v ON t.video_id = v.video_id " +
                "JOIN video_providers vp ON v.provider_id = vp.provider_id " +
                "WHERE vp.provider_slug = ? AND v.native_provider_id = ? AND t.language = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, providerSlug);
            stmt.setString(2, nativeProviderId);
            stmt.setString(3, language);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }
*/
    // ============== User Methods ==============
    public Result<Result<String>> checkUserExist(
            String username,
            String email,
            byte[] passwordHash,
            String displayName,
            String avatarUrl
    ) {
        try {
            return executeQuery(conn -> {

                final String query = "{call check_user_exist(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);

                stmt.setString(1, username);
                stmt.setString(2, email);

                // passwd_hash BYTEA
                if (passwordHash != null) {
                    stmt.setBytes(3, passwordHash);
                } else {
                    stmt.setNull(3, Types.BINARY);
                }

                stmt.setString(4, displayName);

                if (avatarUrl != null) {
                    stmt.setString(5, avatarUrl);
                } else {
                    stmt.setNull(5, Types.VARCHAR);
                }

                // Optional params (use defaults / explicit values)
                stmt.setString(6, "user");     // p_role
                stmt.setString(7, "active");
                // p_status
                stmt.setString(8, "local");    // p_auth_provider

                // p_user_meta JSONB
                stmt.setNull(9, Types.OTHER);  // or Types.JAVA_OBJECT depending on driver

                stmt.execute();
                return Result.success("User checked / inserted / updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING,
                    "Timeout checking user: username=" + username + ", email=" + email, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE,
                    "SQL error checking user: username=" + username + ", email=" + email, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING,
                    "Invalid argument for checkUserExist: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Unexpected error checking user: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> checkVideoExist(
            String providerSlug,
            String nativeProvid,
            String providerName,
            String uploadMetaJson,
            String coreMetaJson,
            String videoMetaJson
    ) {
        try {
            return executeQuery(conn -> {

                final String query =
                        "{call check_video_exist(?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)}";

                CallableStatement stmt = conn.prepareCall(query);

                // Required params
                stmt.setString(1, providerSlug);
                stmt.setString(2, nativeProvid);
                stmt.setString(3, providerName);

                // upload_meta JSONB
                if (uploadMetaJson != null) {
                    stmt.setString(4, uploadMetaJson);
                } else {
                    stmt.setNull(4, Types.OTHER);
                }

                // core_meta JSONB
                if (coreMetaJson != null) {
                    stmt.setString(5, coreMetaJson);
                } else {
                    stmt.setNull(5, Types.OTHER);
                }

                // video_meta JSONB
                if (videoMetaJson != null) {
                    stmt.setString(6, videoMetaJson);
                } else {
                    stmt.setNull(6, Types.OTHER);
                }

                stmt.execute();
                return Result.success("Video checked / inserted / updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING,
                    "Timeout checking video: provider=" + providerSlug +
                            ", nativeProvid=" + nativeProvid, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE,
                    "SQL error checking video: provider=" + providerSlug +
                            ", nativeProvid=" + nativeProvid, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING,
                    "Invalid argument for checkVideoExist: provider=" + providerSlug +
                            ", nativeProvid=" + nativeProvid, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Unexpected error checking video: provider=" + providerSlug +
                            ", nativeProvid=" + nativeProvid, e);
            return Result.failure(e);
        }
    }



    public Result<Result<String>> registerUser(String username, String email, byte[] passwordHash, String displayName, String avatarUrl) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_user(?, ?, ?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setBytes(3, passwordHash);
                stmt.setString(4, displayName);
                if (avatarUrl != null) {
                        stmt.setString(5, avatarUrl);
                } else {
                        stmt.setNull(5, Types.VARCHAR);
                }
                stmt.execute();
                return Result.success("User registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering user: username=" + username + ", email=" + email, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering user: username=" + username + ", email=" + email, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerUser: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering user: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> verifyUser(String username, String email) {
        try {
            return executeQuery(conn -> {

                final String query = "{call verify_user(?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.execute();
                return Result.success("User verified successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout verifying user: username=" + username + ", email=" + email, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error verifying user: username=" + username + ", email=" + email, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for verifyUser: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error verifying user: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateUserLogin(String username, String email, String loginIp) {
        try {
            return executeQuery(conn -> {
                final String query = "{call update_user_login(?, ?, ?::inet)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, loginIp);
                stmt.execute();
                return Result.success("User updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating user login: username=" + username + ", email=" + email, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating user login: username=" + username + ", email=" + email, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateUserLogin: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating user login: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateUserStatus(String username, String email, String status) {
        try {
            return executeQuery(conn -> {

                final String query = "{call update_user_status(?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, status);
                stmt.execute();

                return Result.success("User updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating user status: username=" + username + ", email=" + email + ", status=" + status, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating user status: username=" + username + ", email=" + email + ", status=" + status, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateUserStatus: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating user status: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> upsertUserMeta(String username, String email, String metaKey, String metaValue, String clientType) {
        try {
            return executeQuery(conn -> {

                final String query = "{call upsert_usermeta(?, ?, ?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, metaKey);
                stmt.setString(4, metaValue);
                stmt.setString(5, clientType);
                stmt.execute();

                return Result.success("User updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout upserting user meta: username=" + username + ", email=" + email + ", key=" + metaKey, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error upserting user meta: username=" + username + ", email=" + email + ", key=" + metaKey, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for upsertUserMeta: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error upserting user meta: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> deleteUserMeta(String username, String email, String metaKey, String clientType) {
        try {
            return executeQuery(conn -> {

                final String query = "{call delete_usermeta(?, ?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, metaKey);
                stmt.setString(4, clientType);
                stmt.execute();
                return Result.success("User deleted successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout deleting user meta: username=" + username + ", email=" + email + ", key=" + metaKey, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error deleting user meta: username=" + username + ", email=" + email + ", key=" + metaKey, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for deleteUserMeta: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting user meta: username=" + username + ", email=" + email, e);
            return Result.failure(e);
        }
    }

    // ============== Video Provider Methods ==============

    public Result<Result<String>> registerVideoProvider(String name, String slug, String uri) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_video_provider(?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, name);
                stmt.setString(2, slug);
                stmt.setString(3, uri);
                stmt.execute();
                return Result.success("Video provider registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering video provider: slug=" + slug, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering video provider: slug=" + slug, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerVideoProvider: slug=" + slug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering video provider: slug=" + slug, e);
            return Result.failure(e);
        }
    }

    // ============== Video Methods ==============

    public Result<Result<String>> registerVideoCore(int width, int height, boolean isVertical, double frameRate,
                                          long durationMs, Timestamp startTime, Timestamp endTime,
                                          long fileSizeBytes, String audioCodec, String videoCodec) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_video_core(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setInt(1, width);
                stmt.setInt(2, height);
                stmt.setBoolean(3, isVertical);
                stmt.setDouble(4, frameRate);
                stmt.setLong(5, durationMs);
                stmt.setTimestamp(6, startTime);
                stmt.setTimestamp(7, endTime);
                stmt.setLong(8, fileSizeBytes);
                stmt.setString(9, audioCodec);
                stmt.setString(10, videoCodec);
                stmt.execute();

                return Result.success("Video core registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering video core: width=" + width + ", height=" + height, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering video core: width=" + width + ", height=" + height, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerVideoCore", e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering video core", e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> registerVideoUpload(String providerSlug, String nativeProviderId, String originalUrl,
                                            Timestamp publishedAt, String sourceUrl, Timestamp capturedAt, String providerMeta) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_video_upload(?, ?, ?, ?, ?, ?, ?::jsonb)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, providerSlug);
                stmt.setString(2, nativeProviderId);
                stmt.setString(3, originalUrl);
                stmt.setTimestamp(4, publishedAt);
                stmt.setString(5, sourceUrl);
                stmt.setTimestamp(6, capturedAt);
                stmt.setString(7, providerMeta);
                stmt.execute();

                return Result.success("Video upload registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering video upload: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering video upload: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerVideoUpload: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering video upload: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> markUploadDeleted(String providerSlug, String nativeProviderId) {
        try {
            return executeQuery(conn -> {

                final String query = "{call mark_upload_deleted(?, ?)}";
                CallableStatement stmt = conn.prepareCall(query);
                stmt.setString(1, providerSlug);
                stmt.setString(2, nativeProviderId);
                stmt.execute();
                return Result.success("Upload marked as deleted successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout marking upload deleted: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error marking upload deleted: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for markUploadDeleted: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error marking upload deleted: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> registerVideo(String providerSlug, String nativeProviderId, int width, int height,
                                      boolean isVertical, double frameRate, long durationMs, Timestamp startTime,
                                      Timestamp endTime, long fileSizeBytes, String audioCodec, String videoCodec,
                                      String title, String description, String thumbnailUrl, String langDefault, String contentType) {
        try {
            return executeQuery(conn -> {
                    final String query = "{call register_video(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
                    CallableStatement stmt = conn.prepareCall(query);
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setInt(3, width);
                    stmt.setInt(4, height);
                    stmt.setBoolean(5, isVertical);
                    stmt.setDouble(6, frameRate);
                    stmt.setLong(7, durationMs);
                    stmt.setTimestamp(8, startTime);
                    stmt.setTimestamp(9, endTime);
                    stmt.setLong(10, fileSizeBytes);
                    stmt.setString(11, audioCodec);
                    stmt.setString(12, videoCodec);
                    stmt.setString(13, title);
                    stmt.setString(14, description);
                    stmt.setString(15, thumbnailUrl);
                    stmt.setString(16, langDefault);
                    stmt.setString(17, contentType);
                    stmt.execute();
                    return Result.success("Video registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering video: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering video: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerVideo: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering video: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateVideo(String providerSlug, String nativeProviderId, String title, String description, String thumbnailUrl) {
        try {
            return executeQuery(conn -> {

                final String query = "{call update_video(?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, title);
                    stmt.setString(4, description);
                    stmt.setString(5, thumbnailUrl);
                    stmt.execute();
                }
                return Result.success("Video updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating video: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating video: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateVideo: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating video: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateVideoVisibility(String providerSlug, String nativeProviderId, String visibility) {
        try {
            return executeQuery(conn -> {

                final String query = "{call update_video_visibility(?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, visibility);
                    stmt.execute();
                }
                return Result.success("Video updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating video visibility: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating video visibility: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateVideoVisibility: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating video visibility: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateVideoTracks(String providerSlug, String nativeProviderId, boolean hasAudio, boolean hasCaptions, boolean hasTranscript) {
        try {
            return executeQuery(conn -> {
                    final String query = "{call update_video_tracks(?, ?, ?, ?, ?)}";
                    CallableStatement stmt = conn.prepareCall(query);
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setBoolean(3, hasAudio);
                    stmt.setBoolean(4, hasCaptions);
                    stmt.setBoolean(5, hasTranscript);
                    stmt.execute();

                    return Result.success("Video updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating video tracks: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating video tracks: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateVideoTracks: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating video tracks: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    // ============== Category & Tag Methods ==============

    public Result<Result<String>> registerCategory(String name) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_category(?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, name);
                    stmt.execute();
                }
                return Result.success("Category registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering category: name=" + name, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering category: name=" + name, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerCategory: name=" + name, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering category: name=" + name, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> registerTag(String name) {
        try {
            return executeQuery(conn -> {

                    final String query = "{call register_tag(?)}";
                    CallableStatement stmt = conn.prepareCall(query);
                    stmt.setString(1, name);
                    stmt.execute();

                    return Result.success("Tag registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering tag: name=" + name, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering tag: name=" + name, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerTag: name=" + name, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering tag: name=" + name, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> assignVideoCategory(String providerSlug, String nativeProviderId, String categoryName) {
        try {
            return executeQuery(conn -> {
                    final String query = "{call assign_video_category(?, ?, ?)}";
                    CallableStatement stmt = conn.prepareCall(query);
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, categoryName);
                    stmt.execute();

                    return Result.success("Category assigned successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout assigning video category: provider=" + providerSlug + ", id=" + nativeProviderId + ", category=" + categoryName, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error assigning video category: provider=" + providerSlug + ", id=" + nativeProviderId + ", category=" + categoryName, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for assignVideoCategory: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error assigning video category: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> assignVideoTag(String providerSlug, String nativeProviderId, String tagName) {
        try {
            return executeQuery(conn -> {

                    final String query = "{call assign_video_tag(?, ?, ?)}";
                    CallableStatement stmt = conn.prepareCall(query);
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, tagName);
                    stmt.execute();

                    return Result.success("Video tag assigned successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout assigning video tag: provider=" + providerSlug + ", id=" + nativeProviderId + ", tag=" + tagName, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error assigning video tag: provider=" + providerSlug + ", id=" + nativeProviderId + ", tag=" + tagName, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for assignVideoTag: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error assigning video tag: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> removeVideoCategory(String providerSlug, String nativeProviderId, String categoryName) {
        try {
            return executeQuery(conn -> {

                    final String query = "{call remove_video_category(?, ?, ?)}";
                    CallableStatement stmt = conn.prepareCall(query);
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, categoryName);
                    stmt.execute();
                    return Result.success("Video category removed successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout removing video category: provider=" + providerSlug + ", id=" + nativeProviderId + ", category=" + categoryName, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error removing video category: provider=" + providerSlug + ", id=" + nativeProviderId + ", category=" + categoryName, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for removeVideoCategory: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error removing video category: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> removeVideoTag(String providerSlug, String nativeProviderId, String tagName) {
        try {
            return executeQuery(conn -> {

                final String query = "{call remove_video_tag(?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, tagName);
                    stmt.execute();
                }
                return Result.success("Video tag removed successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout removing video tag: provider=" + providerSlug + ", id=" + nativeProviderId + ", tag=" + tagName, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error removing video tag: provider=" + providerSlug + ", id=" + nativeProviderId + ", tag=" + tagName, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for removeVideoTag: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error removing video tag: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    // ============== Audio & Caption Methods ==============

    public Result<Result<String>> registerAudioTrack(String providerSlug, String nativeProviderId, String soundTitle,
                                           long durationMs, String language, String audioCodec, String fileUrl) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_audio_track(?, ?, ?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, soundTitle);
                    stmt.setLong(4, durationMs);
                    stmt.setString(5, language);
                    stmt.setString(6, audioCodec);
                    stmt.setString(7, fileUrl);
                    stmt.execute();
                }
                return Result.success("Audio track registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering audio track: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering audio track: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerAudioTrack: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering audio track: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> registerCaptionTrack(String providerSlug, String nativeProviderId, String kind,
                                             String language, String source, String format, boolean isOriginal, String fileUrl) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_caption_track(?, ?, ?, ?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, kind);
                    stmt.setString(4, language);
                    stmt.setString(5, source);
                    stmt.setString(6, format);
                    stmt.setBoolean(7, isOriginal);
                    stmt.setString(8, fileUrl);
                    stmt.execute();
                }
                return Result.success("Caption track registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering caption track: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering caption track: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerCaptionTrack: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering caption track: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateCaptionText(String providerSlug, String nativeProviderId, String language, String rawText, String trackText) {
        try {
            return executeQuery(conn -> {

                final String query = "{call update_caption_text(?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, language);
                    stmt.setString(4, rawText);
                    stmt.setString(5, trackText);
                    stmt.execute();
                }
                return Result.success("Caption text updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating caption text: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating caption text: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateCaptionText: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating caption text: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> registerCaptionSegment(String providerSlug, String nativeProviderId, String language,
                                               int position, long startMs, long endMs, String langMain, String text, Double confidenceScore) {
        try {
            return executeQuery(conn -> {

                final String query = "{call register_caption_segment(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, language);
                    stmt.setInt(4, position);
                    stmt.setLong(5, startMs);
                    stmt.setLong(6, endMs);
                    stmt.setString(7, langMain);
                    stmt.setString(8, text);
                    if (confidenceScore != null) {
                        stmt.setDouble(9, confidenceScore);
                    } else {
                        stmt.setNull(9, Types.NUMERIC);
                    }
                    stmt.execute();
                }
                return Result.success("Caption segment registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering caption segment: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering caption segment: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerCaptionSegment: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering caption segment: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    // ============== Transcript Methods ==============

    public Result<Result<String>> registerTranscript(String providerSlug, String nativeProviderId, String captionLanguage,
                                           String audioLanguage, boolean isOriginal, String langMain,
                                           long startOffset, long endOffset, String transcript, Double confidenceScore) {
        try {
            return executeQuery(conn -> {

                if (startOffset < 0 || endOffset < 0 || endOffset < startOffset) {
                    throw new InvalidDataException("Invalid time range: start=" + startOffset + ", end=" + endOffset);
                }

                final String query = "{call register_transcript(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, captionLanguage);
                    stmt.setString(4, audioLanguage);
                    stmt.setBoolean(5, isOriginal);
                    stmt.setString(6, langMain);
                    stmt.setLong(7, startOffset);
                    stmt.setLong(8, endOffset);
                    stmt.setString(9, transcript);
                    if (confidenceScore != null) {
                        stmt.setDouble(10, confidenceScore);
                    } else {
                        stmt.setNull(10, Types.NUMERIC);
                    }
                    stmt.execute();
                }
                return Result.success("Transcript registered successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout registering transcript: provider=" + providerSlug + ", id=" + nativeProviderId, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error registering transcript: provider=" + providerSlug + ", id=" + nativeProviderId, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for registerTranscript: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error registering transcript: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }

    public Result<Result<String>> updateTranscript(String providerSlug, String nativeProviderId, String language, String transcript, Double confidenceScore) {
        try {
            return executeQuery(conn -> {

                final String query = "{call update_transcript(?, ?, ?, ?, ?)}";
                try (CallableStatement stmt = conn.prepareCall(query)) {
                    stmt.setString(1, providerSlug);
                    stmt.setString(2, nativeProviderId);
                    stmt.setString(3, language);
                    stmt.setString(4, transcript);
                    if (confidenceScore != null) {
                        stmt.setDouble(5, confidenceScore);
                    } else {
                        stmt.setNull(5, Types.NUMERIC);
                    }
                    stmt.execute();
                }
                return Result.success("Transcript updated successfully");
            });
        } catch (SQLTimeoutException te) {
            LOGGER.log(Level.WARNING, "Timeout updating transcript: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, te);
            return Result.failure(te);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "SQL error updating transcript: provider=" + providerSlug + ", id=" + nativeProviderId + ", lang=" + language, se);
            return Result.failure(se);
        } catch (IllegalArgumentException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for updateTranscript: provider=" + providerSlug, e);
            return Result.failure(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating transcript: provider=" + providerSlug, e);
            return Result.failure(e);
        }
    }
}
