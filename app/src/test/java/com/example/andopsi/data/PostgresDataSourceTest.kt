package com.example.andopsi.data

import com.example.andopsi.data.PostgresDataSource.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.sql.*
import org.junit.jupiter.api.Assertions.*

class PostgresDataSourceTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setupMocks(): Triple<Connection, CallableStatement, PreparedStatement> {
        // 1. DELETE THESE LINES:
        // mockkStatic("java.lang.Class")
        // every { Class.forName(any()) } returns Any::class.java

        // 2. Keep the DriverManager mock
        mockkStatic(DriverManager::class)
        val mockConn = mockk<Connection>(relaxed = true)
        every { DriverManager.getConnection(any<String>(), any<String>(), any<String>()) } returns mockConn

        // Also mock the Properties version if your code uses it
        every { DriverManager.getConnection(any<String>(), any()) } returns mockConn

        // 3. Mock prepareCall for stored procedures
        val mockCallStmt = mockk<CallableStatement>(relaxed = true)
        every { mockConn.prepareCall(any<String>()) } returns mockCallStmt

        // 4. Mock prepareStatement for standard queries
        val mockPrepStmt = mockk<PreparedStatement>(relaxed = true)
        every { mockConn.prepareStatement(any<String>()) } returns mockPrepStmt

        // 5. Default ResultSet behavior
        val mockResultSet = mockk<ResultSet>(relaxed = true)
        every { mockResultSet.next() } returns true
        every { mockResultSet.getBoolean(1) } returns true
        every { mockPrepStmt.executeQuery() } returns mockResultSet

        return Triple(mockConn, mockCallStmt, mockPrepStmt)
    }

    private fun setupEntityDoesNotExist(prepStmt: PreparedStatement) {
        val mockResultSet = mockk<ResultSet>(relaxed = true)
        every { mockResultSet.next() } returns true
        every { mockResultSet.getBoolean(1) } returns false
        every { prepStmt.executeQuery() } returns mockResultSet
    }

    // ============== User Tests ==============
    @Test
    fun `checkUserExist - local user success`() = runTest {
        // Arrange
        val (conn, stmt, _) = setupMocks()

        val username = "jdoe"
        val email = "jdoe@example.com"
        val passwordHash = byteArrayOf(1, 2, 3, 4)
        val displayName = "John Doe"
        val avatarUrl = "https://example.com/avatar.png"

        val ds = PostgresDataSource()

        // Act
        val result = ds.checkUserExist(
            username,
            email,
            passwordHash,
            displayName,
            avatarUrl
        )

        // Assert
        assertTrue(result.success)
        assertNull(result.error)

        // Verify stored procedure call
        verify { conn.prepareCall("{call check_user_exist(?, ?, ?, ?, ?, ?, ?, ?, ?)}") }
        verify { stmt.setString(1, username) }
        verify { stmt.setString(2, email) }
        verify { stmt.setBytes(3, passwordHash) }
        verify { stmt.setString(4, displayName) }
        verify { stmt.setString(5, avatarUrl) }
        verify { stmt.setString(6, "user") }       // default role
        verify { stmt.setString(7, "active") }    // default status
        verify { stmt.setString(8, "local") }      // default auth_provider
        verify { stmt.setNull(9, java.sql.Types.OTHER) }  // user_meta null
        verify { stmt.execute() }
    }

    @Test
    fun `checkVideoExist - success`() = runTest {
        val (conn, stmt, _) = setupMocks()

        val ds = PostgresDataSource()
        val result = ds.checkVideoExist(
            "youtube",
            "abc123",
            "YouTube",
            """{"original_url":"https://youtube.com"}""",
            """{"width_px":1920,"height_px":1080}""",
            """{"title":"Test Video"}"""
        )

        assertTrue(result.success)
        assertNull(result.error)

        verify {
            conn.prepareCall(
                "{call check_video_exist(?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)}"
            )
        }

        verify { stmt.setString(1, "youtube") }
        verify { stmt.setString(2, "abc123") }
        verify { stmt.setString(3, "YouTube") }
        verify { stmt.execute() }
    }


    @Test
    fun `registerUser - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        // User should not exist for registration
        val prepStmt = mockk<PreparedStatement>(relaxed = true)
        every { conn.prepareStatement(match { it.contains("SELECT EXISTS") && it.contains("users") }) } returns prepStmt
        setupEntityDoesNotExist(prepStmt)

        val ds = PostgresDataSource()
        val result = ds.registerUser("john", "john@example.com", "password".toByteArray(), "John Doe", null)

        assertTrue(result.success)
        assertNull(result.error)
        verify { conn.prepareCall("{call register_user(?, ?, ?, ?, ?)}") }
        verify { stmt.setString(1, "john") }
        verify { stmt.setString(2, "john@example.com") }
        verify { stmt.setBytes(3, "password".toByteArray()) }
        verify { stmt.execute() }
    }

    @Test
    fun `registerUser - user already exists`() = runTest {

        val (conn, stmt, _) = setupMocks()

        val duplicateException = SQLException("ERROR: duplicate key value violates unique constraint", "23505")

        every { conn.prepareCall(any()) } returns stmt
        every { stmt.execute() } throws duplicateException

        val ds = PostgresDataSource()
        val result = ds.registerUser("john", "john@example.com", "password".toByteArray(), "John Doe", null)


        assertFalse(result.success, "Result should be failure")
        assertNotNull(result.error, "Error should not be null")


    }

    @Test
    fun `registerUser - SQL timeout`() = runTest {
        val (_, stmt, _) = setupMocks()
        every { stmt.execute() } throws SQLTimeoutException("Timeout")

        val ds = PostgresDataSource()
        val result = ds.registerUser("john", "john@example.com", "password".toByteArray(), "John Doe", null)

        assertFalse(result.success)
        assertTrue(result.error is SQLTimeoutException)
    }

    @Test
    fun `verifyUser - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.verifyUser("john", "john@example.com")

        assertTrue(result.success)
        assertNull(result.error)
        verify { conn.prepareCall("{call verify_user(?, ?)}") }
        verify { stmt.execute() }
    }


    @Test
    fun `updateUserLogin - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateUserLogin("john", "john@example.com", "192.168.1.1")

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_user_login(?, ?, ?::inet)}") }
        verify { stmt.setString(3, "192.168.1.1") }
    }

    @Test
    fun `updateUserStatus - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateUserStatus("john", "john@example.com", "active")

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_user_status(?, ?, ?)}") }
        verify { stmt.setString(3, "active") }
    }

    @Test
    fun `upsertUserMeta - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.upsertUserMeta("john", "john@example.com", "theme", "dark", "mobile")

        assertTrue(result.success)
        verify { conn.prepareCall("{call upsert_usermeta(?, ?, ?, ?, ?)}") }
        verify { stmt.setString(3, "theme") }
        verify { stmt.setString(4, "dark") }
    }


    @Test
    fun `deleteUserMeta - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.deleteUserMeta("john", "john@example.com", "theme", "mobile")

        assertTrue(result.success)
        verify { conn.prepareCall("{call delete_usermeta(?, ?, ?, ?)}") }
        verify { stmt.setString(3, "theme") }
    }





    // ============== Video Provider Tests ==============

    @Test
    fun `registerVideoProvider - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val prepStmt = mockk<PreparedStatement>(relaxed = true)
        every { conn.prepareStatement(match { it.contains("video_providers") }) } returns prepStmt
        setupEntityDoesNotExist(prepStmt)

        val ds = PostgresDataSource()
        val result = ds.registerVideoProvider("YouTube", "youtube", "https://youtube.com")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_video_provider(?, ?, ?)}") }
        verify { stmt.setString(2, "youtube") }
    }



    // ============== Video Core Tests ==============

    @Test
    fun `registerVideoCore - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerVideoCore(1920, 1080, false, 30.0, 60000L, null, null, 1024000L, "aac", "h264")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_video_core(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}") }
        verify { stmt.setInt(1, 1920) }
        verify { stmt.setInt(2, 1080) }
    }



    // ============== Video Upload Tests ==============

    @Test
    fun `registerVideoUpload - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerVideoUpload("youtube", "abc123", "https://youtube.com/watch?v=abc123",
            null, "https://source.com", null, "{\"key\":\"value\"}")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_video_upload(?, ?, ?, ?, ?, ?, ?::jsonb)}") }
        verify { stmt.setString(7, "{\"key\":\"value\"}") }
    }



    @Test
    fun `markUploadDeleted - case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.markUploadDeleted("youtube", "abc123")

        assertTrue(result.success)
        verify { conn.prepareCall("{call mark_upload_deleted(?, ?)}") }
    }



    /*// ============== Video Tests ==============

    @Test
    fun `registerVideo - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        // Provider exists
        val providerStmt = mockk<PreparedStatement>(relaxed = true)
        val providerRs = mockk<ResultSet>(relaxed = true)
        every { providerRs.next() } returns true
        every { providerRs.getBoolean(1) } returns true
        every { providerStmt.executeQuery() } returns providerRs
        every { conn.prepareStatement(match { it.contains("video_providers") }) } returns providerStmt

        // Video does not exist
        val videoStmt = mockk<PreparedStatement>(relaxed = true)
        val videoRs = mockk<ResultSet>(relaxed = true)
        every { videoRs.next() } returns true
        every { videoRs.getBoolean(1) } returns false
        every { videoStmt.executeQuery() } returns videoRs
        every { conn.prepareStatement(match { it.contains("videos") }) } returns videoStmt

        val ds = PostgresDataSource()
        val result = ds.registerVideo("youtube", "abc123", 1920, 1080, false, 30.0, 60000L,
            null, null, 1024000L, "aac", "h264", "Test Video", "Description", "thumb.jpg", "en", "video")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_video(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}") }
    }

    @Test
    fun `registerVideo - video already exists`() = runTest {
        val (conn, _, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerVideo("youtube", "abc123", 1920, 1080, false, 30.0, 60000L,
            null, null, 1024000L, "aac", "h264", "Test Video", "Description", "thumb.jpg", "en", "video")

        assertFalse(result.success)
        assertTrue(result.error is VideoAlreadyExistsException)
    }

    @Test
    fun `updateVideo - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateVideo("youtube", "abc123", "New Title", "New Description", "new_thumb.jpg")

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_video(?, ?, ?, ?, ?)}") }
        verify { stmt.setString(3, "New Title") }
    }

    @Test
    fun `updateVideo - video not found`() = runTest {
        val (conn, _, _) = setupMocks()
        // Provider exists
        val providerStmt = mockk<PreparedStatement>(relaxed = true)
        val providerRs = mockk<ResultSet>(relaxed = true)
        every { providerRs.next() } returns true
        every { providerRs.getBoolean(1) } returns true
        every { providerStmt.executeQuery() } returns providerRs
        every { conn.prepareStatement(match { it.contains("video_providers") }) } returns providerStmt

        // Video does not exist
        val videoStmt = mockk<PreparedStatement>(relaxed = true)
        val videoRs = mockk<ResultSet>(relaxed = true)
        every { videoRs.next() } returns true
        every { videoRs.getBoolean(1) } returns false
        every { videoStmt.executeQuery() } returns videoRs
        every { conn.prepareStatement(match { it.contains("videos") }) } returns videoStmt

        val ds = PostgresDataSource()
        val result = ds.updateVideo("youtube", "abc123", "New Title", "New Description", "new_thumb.jpg")

        assertFalse(result.success)
        assertTrue(result.error is VideoNotFoundException)
    }

    @Test
    fun `updateVideoVisibility - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateVideoVisibility("youtube", "abc123", "public")

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_video_visibility(?, ?, ?)}") }
        verify { stmt.setString(3, "public") }
    }

    @Test
    fun `updateVideoTracks - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateVideoTracks("youtube", "abc123", true, true, false)

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_video_tracks(?, ?, ?, ?, ?)}") }
        verify { stmt.setBoolean(3, true) }
        verify { stmt.setBoolean(4, true) }
        verify { stmt.setBoolean(5, false) }
    }

    // ============== Category Tests ==============

    @Test
    fun `registerCategory - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val prepStmt = mockk<PreparedStatement>(relaxed = true)
        every { conn.prepareStatement(match { it.contains("categories") }) } returns prepStmt
        setupEntityDoesNotExist(prepStmt)

        val ds = PostgresDataSource()
        val result = ds.registerCategory("Music")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_category(?)}") }
        verify { stmt.setString(1, "Music") }
    }

    @Test
    fun `registerCategory - already exists`() = runTest {
        val (conn, _, _) = setupMocks()
        val prepStmt = mockk<PreparedStatement>(relaxed = true)
        val mockResultSet = mockk<ResultSet>(relaxed = true)
        every { mockResultSet.next() } returns true
        every { mockResultSet.getBoolean(1) } returns true
        every { prepStmt.executeQuery() } returns mockResultSet
        every { conn.prepareStatement(match { it.contains("categories") }) } returns prepStmt

        val ds = PostgresDataSource()
        val result = ds.registerCategory("Music")

        assertFalse(result.success)
        assertTrue(result.error is CategoryAlreadyExistsException)
    }

    // ============== Tag Tests ==============

    @Test
    fun `registerTag - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val prepStmt = mockk<PreparedStatement>(relaxed = true)
        every { conn.prepareStatement(match { it.contains("tags") }) } returns prepStmt
        setupEntityDoesNotExist(prepStmt)

        val ds = PostgresDataSource()
        val result = ds.registerTag("tutorial")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_tag(?)}") }
    }

    @Test
    fun `registerTag - already exists`() = runTest {
        val (conn, _, _) = setupMocks()
        val prepStmt = mockk<PreparedStatement>(relaxed = true)
        val mockResultSet = mockk<ResultSet>(relaxed = true)
        every { mockResultSet.next() } returns true
        every { mockResultSet.getBoolean(1) } returns true
        every { prepStmt.executeQuery() } returns mockResultSet
        every { conn.prepareStatement(match { it.contains("tags") }) } returns prepStmt

        val ds = PostgresDataSource()
        val result = ds.registerTag("tutorial")

        assertFalse(result.success)
        assertTrue(result.error is TagAlreadyExistsException)
    }

    @Test
    fun `assignVideoCategory - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.assignVideoCategory("youtube", "abc123", "Music")

        assertTrue(result.success)
        verify { conn.prepareCall("{call assign_video_category(?, ?, ?)}") }
    }

    @Test
    fun `assignVideoCategory - category not found`() = runTest {
        val (conn, _, _) = setupMocks()
        // Provider exists
        val providerStmt = mockk<PreparedStatement>(relaxed = true)
        val providerRs = mockk<ResultSet>(relaxed = true)
        every { providerRs.next() } returns true
        every { providerRs.getBoolean(1) } returns true
        every { providerStmt.executeQuery() } returns providerRs
        every { conn.prepareStatement(match { it.contains("video_providers") }) } returns providerStmt

        // Video exists
        val videoStmt = mockk<PreparedStatement>(relaxed = true)
        val videoRs = mockk<ResultSet>(relaxed = true)
        every { videoRs.next() } returns true
        every { videoRs.getBoolean(1) } returns true
        every { videoStmt.executeQuery() } returns videoRs
        every { conn.prepareStatement(match { it.contains("videos") }) } returns videoStmt

        // Category does not exist
        val categoryStmt = mockk<PreparedStatement>(relaxed = true)
        val categoryRs = mockk<ResultSet>(relaxed = true)
        every { categoryRs.next() } returns true
        every { categoryRs.getBoolean(1) } returns false
        every { categoryStmt.executeQuery() } returns categoryRs
        every { conn.prepareStatement(match { it.contains("categories") }) } returns categoryStmt

        val ds = PostgresDataSource()
        val result = ds.assignVideoCategory("youtube", "abc123", "Music")

        assertFalse(result.success)
        assertTrue(result.error is CategoryNotFoundException)
    }

    @Test
    fun `assignVideoTag - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.assignVideoTag("youtube", "abc123", "tutorial")

        assertTrue(result.success)
        verify { conn.prepareCall("{call assign_video_tag(?, ?, ?)}") }
    }

    @Test
    fun `removeVideoCategory - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.removeVideoCategory("youtube", "abc123", "Music")

        assertTrue(result.success)
        verify { conn.prepareCall("{call remove_video_category(?, ?, ?)}") }
    }

    @Test
    fun `removeVideoTag - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.removeVideoTag("youtube", "abc123", "tutorial")

        assertTrue(result.success)
        verify { conn.prepareCall("{call remove_video_tag(?, ?, ?)}") }
    }

    // ============== Audio Track Tests ==============

    @Test
    fun `registerAudioTrack - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerAudioTrack("youtube", "abc123", "Background Music", 180000L, "en", "aac", "audio.mp3")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_audio_track(?, ?, ?, ?, ?, ?, ?)}") }
        verify { stmt.setLong(4, 180000L) }
    }

    @Test
    fun `registerAudioTrack - invalid duration`() = runTest {
        setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerAudioTrack("youtube", "abc123", "Background Music", -100L, "en", "aac", "audio.mp3")

        assertFalse(result.success)
        assertTrue(result.error is InvalidDataException)
        assertTrue(result.error!!.message!!.contains("Duration"))
    }

    // ============== Caption Track Tests ==============

    @Test
    fun `registerCaptionTrack - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerCaptionTrack("youtube", "abc123", "subtitles", "en", "manual", "vtt", true, "captions.vtt")

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_caption_track(?, ?, ?, ?, ?, ?, ?, ?)}") }
        verify { stmt.setBoolean(7, true) }
    }

    @Test
    fun `updateCaptionText - success case`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateCaptionText("youtube", "abc123", "en", "Raw text", "Formatted text")

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_caption_text(?, ?, ?, ?, ?)}") }
    }

    @Test
    fun `updateCaptionText - caption track not found`() = runTest {
        val (conn, _, _) = setupMocks()
        // Provider exists
        val providerStmt = mockk<PreparedStatement>(relaxed = true)
        val providerRs = mockk<ResultSet>(relaxed = true)
        every { providerRs.next() } returns true
        every { providerRs.getBoolean(1) } returns true
        every { providerStmt.executeQuery() } returns providerRs
        every { conn.prepareStatement(match { it.contains("video_providers") }) } returns providerStmt

        // Video exists
        val videoStmt = mockk<PreparedStatement>(relaxed = true)
        val videoRs = mockk<ResultSet>(relaxed = true)
        every { videoRs.next() } returns true
        every { videoRs.getBoolean(1) } returns true
        every { videoStmt.executeQuery() } returns videoRs
        every { conn.prepareStatement(match { it.contains("videos") && !it.contains("caption_tracks") }) } returns videoStmt

        // Caption track does not exist
        val captionStmt = mockk<PreparedStatement>(relaxed = true)
        val captionRs = mockk<ResultSet>(relaxed = true)
        every { captionRs.next() } returns true
        every { captionRs.getBoolean(1) } returns false
        every { captionStmt.executeQuery() } returns captionRs
        every { conn.prepareStatement(match { it.contains("caption_tracks") }) } returns captionStmt

        val ds = PostgresDataSource()
        val result = ds.updateCaptionText("youtube", "abc123", "en", "Raw text", "Formatted text")

        assertFalse(result.success)
        assertTrue(result.error is CaptionTrackNotFoundException)
    }

    @Test
    fun `registerCaptionSegment - success with confidence`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerCaptionSegment("youtube", "abc123", "en", 1, 0L, 5000L, "en", "Hello world", 0.95)

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_caption_segment(?, ?, ?, ?, ?, ?, ?, ?, ?)}") }
        verify { stmt.setDouble(9, 0.95) }
    }

    @Test
    fun `registerCaptionSegment - invalid time range`() = runTest {
        setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerCaptionSegment("youtube", "abc123", "en", 1, 5000L, 1000L, "en", "Hello world", 0.95)

        assertFalse(result.success)
        assertTrue(result.error is InvalidDataException)
        assertTrue(result.error!!.message!!.contains("Invalid time range"))
    }

    @Test
    fun `registerCaptionSegment - success without confidence`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerCaptionSegment("youtube", "abc123", "en", 1, 0L, 5000L, "en", "Hello world", null)

        assertTrue(result.success)
        verify { stmt.setNull(9, Types.NUMERIC) }
    }

    // ============== Transcript Tests ==============

    @Test
    fun `registerTranscript - success with confidence`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerTranscript("youtube", "abc123", "en", "en", true, "en", 0L, 60000L, "Full transcript text", 0.89)

        assertTrue(result.success)
        verify { conn.prepareCall("{call register_transcript(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}") }
        verify { stmt.setDouble(10, 0.89) }
    }

    @Test
    fun `registerTranscript - success without confidence`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerTranscript("youtube", "abc123", "en", "en", true, "en", 0L, 60000L, "Full transcript text", null)

        assertTrue(result.success)
        verify { stmt.setNull(10, Types.NUMERIC) }
    }

    @Test
    fun `registerTranscript - invalid time range`() = runTest {
        setupMocks()
        val ds = PostgresDataSource()
        val result = ds.registerTranscript("youtube", "abc123", "en", "en", true, "en", 60000L, 30000L, "Full transcript text", 0.89)

        assertFalse(result.success)
        assertTrue(result.error is InvalidDataException)
    }

    @Test
    fun `updateTranscript - success with confidence`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateTranscript("youtube", "abc123", "en", "Updated transcript", 0.92)

        assertTrue(result.success)
        verify { conn.prepareCall("{call update_transcript(?, ?, ?, ?, ?)}") }
        verify { stmt.setDouble(5, 0.92) }
    }

    @Test
    fun `updateTranscript - success without confidence`() = runTest {
        val (conn, stmt, _) = setupMocks()
        val ds = PostgresDataSource()
        val result = ds.updateTranscript("youtube", "abc123", "en", "Updated transcript", null)

        assertTrue(result.success)
        verify { stmt.setNull(5, Types.NUMERIC) }
    }

    @Test
    fun `updateTranscript - transcript not found`() = runTest {
        val (conn, _, _) = setupMocks()
        // Provider exists
        val providerStmt = mockk<PreparedStatement>(relaxed = true)
        val providerRs = mockk<ResultSet>(relaxed = true)
        every { providerRs.next() } returns true
        every { providerRs.getBoolean(1) } returns true
        every { providerStmt.executeQuery() } returns providerRs
        every { conn.prepareStatement(match { it.contains("video_providers") }) } returns providerStmt

        // Video exists
        val videoStmt = mockk<PreparedStatement>(relaxed = true)
        val videoRs = mockk<ResultSet>(relaxed = true)
        every { videoRs.next() } returns true
        every { videoRs.getBoolean(1) } returns true
        every { videoStmt.executeQuery() } returns videoRs
        every { conn.prepareStatement(match { it.contains("videos") && !it.contains("transcripts") }) } returns videoStmt

        // Transcript does not exist
        val transcriptStmt = mockk<PreparedStatement>(relaxed = true)
        val transcriptRs = mockk<ResultSet>(relaxed = true)
        every { transcriptRs.next() } returns true
        every { transcriptRs.getBoolean(1) } returns false
        every { transcriptStmt.executeQuery() } returns transcriptRs
        every { conn.prepareStatement(match { it.contains("transcripts") }) } returns transcriptStmt

        val ds = PostgresDataSource()
        val result = ds.updateTranscript("youtube", "abc123", "en", "Updated transcript", 0.92)

        assertFalse(result.success)
        assertTrue(result.error is TranscriptNotFoundException)
    }

    // ============== SQL Exception Tests ==============

    @Test
    fun `handles SQLException properly`() = runTest {
        val (_, stmt, _) = setupMocks()
        every { stmt.execute() } throws SQLException("Database error", "08001")

        val ds = PostgresDataSource()
        val result = ds.registerCategory("Music")

        assertFalse(result.success)
        assertTrue(result.error is SQLException)
    }

    @Test
    fun `handles NullPointerException properly`() = runTest {
        val (conn, _, _) = setupMocks()
        every { conn.prepareCall(any()) } throws NullPointerException("Null value")

        val ds = PostgresDataSource()
        val result = ds.registerCategory("Music")

        assertFalse(result.success)
        assertTrue(result.error is NullPointerException)
    }*/
}