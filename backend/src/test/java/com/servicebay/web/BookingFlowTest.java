package com.servicebay.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end API test against a real PostgreSQL database (servicebay_test).
 * "Now" is fixed at Monday 9 Nov 2026, 10:00 IST.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingFlowTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock testClock() {
            ZoneId ist = ZoneId.of("Asia/Kolkata");
            return Clock.fixed(LocalDateTime.of(2026, 11, 9, 10, 0).atZone(ist).toInstant(), ist);
        }
    }

    static final String CUSTOMER = "Bearer dev:cust-1:+919876543210:Rahul Sharma";
    static final String CUSTOMER2 = "Bearer dev:cust-2:+919811111111:Priya Nair";
    static final String MERCHANT = "Bearer dev:merch-1:+919000012345:Ravi";
    static final String MERCHANT2 = "Bearer dev:merch-2:+919000099999:Other";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE notification, review, booking, service_offering, merchant_photo, merchant, device_token, app_user RESTART IDENTITY CASCADE");
    }

    @Test
    void fullBookingLifecycle() throws Exception {
        // --- roles ---
        call(post("/api/me/role"), MERCHANT, "{\"role\":\"MERCHANT\",\"name\":\"Ravi\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.merchantOnboarded").value(false));
        call(post("/api/me/role"), CUSTOMER, "{\"role\":\"CUSTOMER\",\"name\":\"Rahul Sharma\"}")
                .andExpect(status().isOk());

        // --- merchant onboarding with timings ---
        call(put("/api/merchant/profile"), MERCHANT, """
                {"shopName":"Speed Auto Care","phone":"+91 90000 12345","address":"#42, 80 Feet Rd, Koramangala",
                 "pincode":"560034","city":"Bengaluru","latitude":12.9352,"longitude":77.6245,
                 "servesBike":true,"servesCar":false,
                 "openDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"],
                 "openTime":"09:00","closeTime":"20:00","slotMinutes":30}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.onboarded").value(true));

        // closing before opening is rejected
        call(put("/api/merchant/profile"), MERCHANT, """
                {"shopName":"X","phone":"9000012345","address":"A","pincode":"560034","servesBike":true,"servesCar":false,
                 "openDays":["MONDAY"],"openTime":"20:00","closeTime":"09:00"}""")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_TIMINGS"));

        // shop photo upload (PNG magic bytes)
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        mvc.perform(multipart("/api/merchant/photos").file(new MockMultipartFile("file", "shop.png", "image/png", png))
                        .header("Authorization", MERCHANT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photos.length()").value(1));
        mvc.perform(multipart("/api/merchant/photos").file(new MockMultipartFile("file", "x.png", "image/png", "not an image".getBytes()))
                        .header("Authorization", MERCHANT))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_FILE_TYPE"));

        // --- services ---
        long washId = body(call(post("/api/merchant/services"), MERCHANT, """
                {"category":"WATER_WASH","vehicleType":"BIKE","name":"Water wash","description":"Foam wash, chain lube",
                 "price":149,"durationMin":30}""").andExpect(status().isCreated())).get("id").asLong();
        call(post("/api/merchant/services"), MERCHANT, """
                {"category":"FULL_SERVICE","vehicleType":"CAR","name":"Car service","price":2499,"durationMin":180}""")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VEHICLE_NOT_SERVED"));

        // --- customer discovery by pincode ---
        call(get("/api/merchants?pincode=560034&vehicle=BIKE"), CUSTOMER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].shopName").value("Speed Auto Care"))
                .andExpect(jsonPath("$[0].fromPrice").value(149))
                .andExpect(jsonPath("$[0].openNow").value(true));
        call(get("/api/merchants?pincode=560001"), CUSTOMER, null).andExpect(jsonPath("$.length()").value(0));
        call(get("/api/merchants?pincode=560034&vehicle=CAR"), CUSTOMER, null).andExpect(jsonPath("$.length()").value(0));
        long shopId = body(call(get("/api/merchants?pincode=560034"), CUSTOMER, null)).get(0).get("id").asLong();

        call(get("/api/merchants/" + shopId), CUSTOMER, null)
                .andExpect(jsonPath("$.shop.openTime").value("09:00:00"))
                .andExpect(jsonPath("$.services[0].price").value(149));

        // Sunday is closed; Tuesday has slots
        call(get("/api/merchants/" + shopId + "/slots?date=2026-11-15"), CUSTOMER, null)
                .andExpect(jsonPath("$.open").value(false)).andExpect(jsonPath("$.slots.length()").value(0));
        call(get("/api/merchants/" + shopId + "/slots?date=2026-11-10&serviceId=" + washId), CUSTOMER, null)
                .andExpect(jsonPath("$.open").value(true)).andExpect(jsonPath("$.slots.length()").value(22));

        // --- book Tue 10 Nov 16:30 ---
        String bookingReq = """
                {"merchantId":%d,"serviceId":%d,"date":"2026-11-10","time":"16:30",
                 "address":"Flat 302, Green Residency, Koramangala","pincode":"560034","phone":"+91 98765 43210"}"""
                .formatted(shopId, washId);
        JsonNode booking = body(call(post("/api/bookings"), CUSTOMER, bookingReq).andExpect(status().isCreated()));
        long bookingId = booking.get("id").asLong();
        String otp = booking.get("otp").asText();
        assertThat(otp).matches("\\d{4}");
        assertThat(booking.get("status").asText()).isEqualTo("REQUESTED");

        // same slot again -> conflict (double booking prevented)
        call(post("/api/me/role"), CUSTOMER2, "{\"role\":\"CUSTOMER\"}").andExpect(status().isOk());
        call(post("/api/bookings"), CUSTOMER2, bookingReq)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        call(get("/api/merchants/" + shopId + "/slots?date=2026-11-10"), CUSTOMER2, null)
                .andExpect(jsonPath("$.slots[?(@.time=='16:30:00')].available").value(false));

        // off-grid and past times are rejected
        call(post("/api/bookings"), CUSTOMER2, bookingReq.replace("16:30", "16:45"))
                .andExpect(status().isConflict());
        call(post("/api/bookings"), CUSTOMER2, bookingReq.replace("2026-11-10", "2026-11-09").replace("16:30", "09:30"))
                .andExpect(status().isConflict());

        // --- merchant sees request with contact hidden, and gets a notification ---
        call(get("/api/merchant/bookings?scope=pending"), MERCHANT, null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerPincode").value("560034"))
                .andExpect(jsonPath("$[0].customerPhone").doesNotExist())
                .andExpect(jsonPath("$[0].customerAddress").doesNotExist())
                .andExpect(jsonPath("$[0].otp").doesNotExist());
        call(get("/api/me/notifications"), MERCHANT, null)
                .andExpect(jsonPath("$.unread").value(1))
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_REQUESTED"));

        // another merchant cannot touch it
        call(post("/api/me/role"), MERCHANT2, "{\"role\":\"MERCHANT\"}").andExpect(status().isOk());
        call(post("/api/merchant/bookings/" + bookingId + "/accept"), MERCHANT2, null).andExpect(status().isNotFound());

        // role enforcement
        call(get("/api/merchant/bookings"), CUSTOMER, null).andExpect(status().isForbidden());
        call(post("/api/bookings"), MERCHANT, bookingReq).andExpect(status().isForbidden());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

        // starting before accepting is not allowed
        call(post("/api/merchant/bookings/" + bookingId + "/start"), MERCHANT, "{\"otp\":\"" + otp + "\"}")
                .andExpect(status().isConflict());

        // --- accept: contact unlocked, customer notified ---
        call(post("/api/merchant/bookings/" + bookingId + "/accept"), MERCHANT, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.customerPhone").value("+91 98765 43210"))
                .andExpect(jsonPath("$.customerAddress").value("Flat 302, Green Residency, Koramangala"));
        call(get("/api/me/notifications"), CUSTOMER, null)
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_ACCEPTED"));

        // --- start with wrong OTP, then right OTP ---
        String wrong = otp.equals("0000") ? "1111" : "0000";
        call(post("/api/merchant/bookings/" + bookingId + "/start"), MERCHANT, "{\"otp\":\"" + wrong + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"))
                .andExpect(jsonPath("$.detail").value("Wrong OTP. 4 attempts left."));
        call(post("/api/merchant/bookings/" + bookingId + "/start"), MERCHANT, "{\"otp\":\"12\"}")
                .andExpect(status().isBadRequest());
        call(post("/api/merchant/bookings/" + bookingId + "/start"), MERCHANT, "{\"otp\":\"" + otp + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // OTP no longer shown to the customer once started
        call(get("/api/bookings"), CUSTOMER, null)
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].otp").doesNotExist());

        // rating not allowed before completion
        call(post("/api/bookings/" + bookingId + "/review"), CUSTOMER, "{\"rating\":5}")
                .andExpect(status().isConflict());

        // --- complete and review ---
        call(post("/api/merchant/bookings/" + bookingId + "/complete"), MERCHANT, null)
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        long reviewId = body(call(post("/api/bookings/" + bookingId + "/review"), CUSTOMER,
                "{\"rating\":4,\"comment\":\"Spotless and on time\"}").andExpect(status().isCreated())).get("id").asLong();
        call(post("/api/bookings/" + bookingId + "/review"), CUSTOMER, "{\"rating\":5}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_REVIEWED"));

        call(get("/api/merchants/" + shopId + "/reviews"), CUSTOMER, null)
                .andExpect(jsonPath("$.ratingAvg").value(4.0))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.distribution.4").value(1))
                .andExpect(jsonPath("$.reviews[0].customerName").value("Rahul S."));

        call(post("/api/merchant/reviews/" + reviewId + "/reply"), MERCHANT, "{\"reply\":\"Thanks Rahul!\"}")
                .andExpect(jsonPath("$.reply").value("Thanks Rahul!"));
        call(get("/api/bookings"), CUSTOMER, null).andExpect(jsonPath("$[0].myRating").value(4));
    }

    @Test
    void otpLocksAfterFiveWrongAttempts() throws Exception {
        long[] ids = setUpAcceptedBooking();
        for (int i = 0; i < 5; i++) {
            call(post("/api/merchant/bookings/" + ids[0] + "/start"), MERCHANT, "{\"otp\":\"" + wrongOtp(ids[1]) + "\"}")
                    .andExpect(status().isBadRequest());
        }
        call(post("/api/merchant/bookings/" + ids[0] + "/start"), MERCHANT, "{\"otp\":\"" + String.format("%04d", ids[1]) + "\"}")
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("OTP_LOCKED"));
    }

    @Test
    void customerCancelFreesSlotAndDeclineNotifies() throws Exception {
        long[] ids = setUpAcceptedBooking();
        call(post("/api/bookings/" + ids[0] + "/cancel"), CUSTOMER, null)
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        long shopId = jdbc.queryForObject("select id from merchant", Long.class);
        call(get("/api/merchants/" + shopId + "/slots?date=2026-11-10"), CUSTOMER, null)
                .andExpect(jsonPath("$.slots[?(@.time=='16:30:00')].available").value(true));
        call(get("/api/me/notifications"), MERCHANT, null)
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_CANCELLED"));
    }

    @Test
    void remindersAreSentOnceForBookingsWithinAnHour() throws Exception {
        long[] ids = setUpAcceptedBooking();
        // move the booking to 10:45 today (within the next hour of the fixed clock)
        jdbc.update("update booking set slot_start = '2026-11-09 10:45' where id = ?", ids[0]);
        assertThat(applicationContextBookingService().sendDueReminders()).isEqualTo(1);
        assertThat(applicationContextBookingService().sendDueReminders()).isZero();
        call(get("/api/me/notifications"), CUSTOMER, null)
                .andExpect(jsonPath("$.items[0].type").value("BOOKING_REMINDER"));
    }

    // ---------- helpers ----------

    @Autowired org.springframework.context.ApplicationContext ctx;

    private com.servicebay.service.BookingService applicationContextBookingService() {
        return ctx.getBean(com.servicebay.service.BookingService.class);
    }

    /** @return {bookingId, otp as number} */
    private long[] setUpAcceptedBooking() throws Exception {
        call(post("/api/me/role"), MERCHANT, "{\"role\":\"MERCHANT\"}");
        call(post("/api/me/role"), CUSTOMER, "{\"role\":\"CUSTOMER\",\"name\":\"Rahul Sharma\"}");
        call(put("/api/merchant/profile"), MERCHANT, """
                {"shopName":"Speed Auto Care","phone":"9000012345","address":"Koramangala","pincode":"560034",
                 "servesBike":true,"servesCar":true,"openDays":["MONDAY","TUESDAY"],"openTime":"09:00","closeTime":"20:00"}""")
                .andExpect(status().isOk());
        long serviceId = body(call(post("/api/merchant/services"), MERCHANT,
                "{\"category\":\"PUNCTURE\",\"vehicleType\":\"BIKE\",\"name\":\"Puncture\",\"price\":99,\"durationMin\":15}"))
                .get("id").asLong();
        long shopId = jdbc.queryForObject("select id from merchant", Long.class);
        JsonNode b = body(call(post("/api/bookings"), CUSTOMER, """
                {"merchantId":%d,"serviceId":%d,"date":"2026-11-10","time":"16:30","address":"Flat 1","pincode":"560034","phone":"9876543210"}"""
                .formatted(shopId, serviceId)).andExpect(status().isCreated()));
        long id = b.get("id").asLong();
        call(post("/api/merchant/bookings/" + id + "/accept"), MERCHANT, null).andExpect(status().isOk());
        return new long[]{id, Long.parseLong(b.get("otp").asText())};
    }

    private static String wrongOtp(long otp) {
        return String.format("%04d", (otp + 1) % 10_000);
    }

    private ResultActions call(MockHttpServletRequestBuilder req, String auth, String bodyJson) throws Exception {
        req.header("Authorization", auth);
        if (bodyJson != null) req.contentType(MediaType.APPLICATION_JSON).content(bodyJson);
        return mvc.perform(req);
    }

    private JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }
}
