package org.prebid.server.bidder.emxdigital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iab.openrtb.request.App;
import com.iab.openrtb.request.Banner;
import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.request.Device;
import com.iab.openrtb.request.Format;
import com.iab.openrtb.request.Imp;
import com.iab.openrtb.request.Site;
import com.iab.openrtb.request.Video;
import com.iab.openrtb.response.Bid;
import com.iab.openrtb.response.BidResponse;
import com.iab.openrtb.response.SeatBid;
import org.junit.Before;
import org.junit.Test;
import org.prebid.server.VertxTest;
import org.prebid.server.bidder.model.BidderBid;
import org.prebid.server.bidder.model.BidderCall;
import org.prebid.server.bidder.model.BidderError;
import org.prebid.server.bidder.model.HttpRequest;
import org.prebid.server.bidder.model.HttpResponse;
import org.prebid.server.bidder.model.Result;
import org.prebid.server.proto.openrtb.ext.ExtPrebid;
import org.prebid.server.proto.openrtb.ext.request.emxdigital.ExtImpEmxDigital;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.prebid.server.proto.openrtb.ext.response.BidType.banner;
import static org.prebid.server.proto.openrtb.ext.response.BidType.video;

public class EmxDigitalBidderTest extends VertxTest {

    private static final String ENDPOINT_URL = "https://test.endpoint.com";

    private EmxDigitalBidder emxDigitalBidder;

    @Before
    public void setUp() {
        emxDigitalBidder = new EmxDigitalBidder(ENDPOINT_URL, jacksonMapper);
    }

    @Test
    public void creationShouldFailOnInvalidEndpointUrl() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmxDigitalBidder("invalid_url", jacksonMapper));
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenImpExtCouldNotBeParsed() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .ext(mapper.valueToTree(ExtPrebid.of(null, mapper.createArrayNode())))
                        .build()))
                .id("request_id")
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("Cannot deserialize value");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenImpNotContainsBanner() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", ""))))
                        .build()))
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("Request needs to include a Banner object");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenImpExtEmxDigitalTagidIsNull() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of(null, ""))))
                        .build()))
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("tagid must be a String of numbers");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenImpExtEmxDigitalTagidIsNotNumber() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("not", ""))))
                        .build()))
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("tagid must be a String of numbers");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenImpExtEmxDigitalTagidIsZero() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("0", ""))))
                        .build()))
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("tagid cant be 0");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenWidthAndHeightIsNullAndBannerFormatIsNull() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("1", "1"))))
                        .build()))
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("Need at least one size to build request");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorWhenWidthAndHeightIsNullAndBannerFormatIsEmpty() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().format(Collections.emptyList()).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("1", "1"))))
                        .build()))
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("Need at least one size to build request");
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeHttpRequestsShouldModifyImpWhenExtImpEmaDigitalContainsRequiredValues() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().w(100).h(100).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .site(Site.builder().page("https://exmaple/").build())
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        final Imp expectedImp = Imp.builder()
                .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                .banner(Banner.builder().w(100).h(100).build())
                .tagid("123")
                .secure(1)
                .bidfloor(new BigDecimal("2"))
                .bidfloorcur("USD")
                .build();

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .flatExtracting(BidRequest::getImp)
                .containsExactly(expectedImp);
    }

    @Test
    public void makeHttpRequestsShouldRemoveVast40ProtocolFromVideo() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .video(Video.builder()
                                .mimes(Collections.singletonList("someMime"))
                                .protocols(Arrays.asList(1, 7, 2))
                                .w(100)
                                .h(100)
                                .build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        final Imp expectedImp = Imp.builder()
                .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                .video(Video.builder()
                        .mimes(Collections.singletonList("someMime"))
                        .protocols(Arrays.asList(1, 2))
                        .w(100)
                        .h(100)
                        .build())
                .tagid("123")
                .secure(0)
                .bidfloor(new BigDecimal("2"))
                .bidfloorcur("USD")
                .build();

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .flatExtracting(BidRequest::getImp)
                .containsExactly(expectedImp);
    }

    @Test
    public void shouldThrowExceptionIfVideoDoNotHaveAtLeastOneSizeParameter() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .video(Video.builder().mimes(Collections.singletonList("someMime")).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1)
                .containsExactly(BidderError.badInput("Video: Need at least one size to build request"));
    }

    @Test
    public void shouldThrowExceptionIfVideoDoNotHaveAnyMimeParameter() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .video(Video.builder().build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1)
                .containsExactly(BidderError.badInput("Video: missing required field mimes"));
    }

    @Test
    public void requestSecureShouldBeOneIfPageStartsWithHttps() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().w(100).h(100).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .site(Site.builder().page("https://exmaple/").build())
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .extracting(request -> request.getImp().get(0).getSecure())
                .containsExactly(1);
    }

    @Test
    public void requestSecureShouldBeOneIfUrlStartsWithHttps() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().w(100).h(100).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .app(App.builder().domain("https://exmaple/").build())
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .flatExtracting(BidRequest::getImp)
                .extracting(Imp::getSecure)
                .containsExactly(1);
    }

    @Test
    public void requestSecureShouldBe1IfStoreUrlStartsWithHttps() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().w(100).h(100).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .app(App.builder().storeurl("https://exmaple/").build())
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .flatExtracting(BidRequest::getImp)
                .extracting(Imp::getSecure)
                .containsExactly(1);
    }

    @Test
    public void requestSecureShouldBe0IfPageDoNotStartsWithHttps() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().w(100).h(100).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("123", "2"))))
                        .build()))
                .tmax(1000L)
                .site(Site.builder().page("http://exmaple/").build())
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .flatExtracting(BidRequest::getImp)
                .extracting(Imp::getSecure)
                .containsExactly(0);
    }

    @Test
    public void makeHttpRequestsShouldModifyBannerFormatAndWidthAndHeightWhenRequestBannerWidthAndHeightIsNull() {
        // given
        final List<Format> formats = Arrays.asList(
                Format.builder().h(20).w(21).build(),
                Format.builder().h(30).w(31).build());

        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().format(formats).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("1", "asd"))))
                        .build()))
                .tmax(1000L)
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        final Banner expectedBanner = Banner.builder().h(20).w(21)
                .format(singletonList(Format.builder().h(30).w(31).build())).build();

        final Imp expectedImp = Imp.builder()
                .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("1", "asd"))))
                .banner(expectedBanner)
                .tagid("1")
                .secure(0)
                .build();

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(httpRequest -> mapper.readValue(httpRequest.getBody(), BidRequest.class))
                .flatExtracting(BidRequest::getImp).hasSize(1)
                .containsExactly(expectedImp);
    }

    @Test
    public void makeHttpRequestsShouldSendRequestToModifiedUrlWithHeaders() {
        // given
        final BidRequest bidRequest = BidRequest.builder()
                .imp(singletonList(Imp.builder()
                        .banner(Banner.builder().w(1).h(1).build())
                        .ext(mapper.valueToTree(ExtPrebid.of(null, ExtImpEmxDigital.of("1", "asd"))))
                        .build()))
                .device(Device.builder().ip("ip").ua("Agent").language("fr").dnt(1).build())
                .site(Site.builder().page("myPage").build())
                .tmax(1000L)
                .build();

        // when
        final Result<List<HttpRequest<BidRequest>>> result = emxDigitalBidder
                .makeHttpRequests(bidRequest);

        // then
        final int expectedTime = (int) Instant.now().getEpochSecond();

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(HttpRequest::getUri)
                .allSatisfy(uri -> {
                    assertThat(uri).startsWith("https://test.endpoint.com?t=1000&ts=");
                    assertThat(uri).endsWith("&src=pbserver");
                    final String ts = uri.substring(36, uri.indexOf("&src"));
                    assertThat(Integer.parseInt(ts)).isCloseTo(expectedTime, within(10));
                });

        assertThat(result.getValue()).hasSize(1)
                .flatExtracting(r -> r.getHeaders().entries())
                .extracting(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactlyInAnyOrder(
                        tuple("Content-Type", "application/json;charset=utf-8"),
                        tuple("Accept", "application/json"),
                        tuple("User-Agent", "Agent"),
                        tuple("X-Forwarded-For", "ip"),
                        tuple("Referer", "myPage"),
                        tuple("DNT", "1"),
                        tuple("Accept-Language", "fr"));
    }

    @Test
    public void makeBidsShouldReturnErrorWhenResponseBodyCouldNotBeParsed() {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(null, "invalid");

        // when
        final Result<List<BidderBid>> result = emxDigitalBidder.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage())
                .startsWith("Failed to decode: Unrecognized token");
        assertThat(result.getErrors().get(0).getType())
                .isEqualTo(BidderError.Type.bad_server_response);
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnEmptyListWhenBidResponseIsNull()
            throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(null,
                mapper.writeValueAsString(null));

        // when
        final Result<List<BidderBid>> result = emxDigitalBidder.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnEmptyListWhenBidResponseSeatBidIsNull()
            throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(null,
                mapper.writeValueAsString(BidResponse.builder().build()));

        // when
        final Result<List<BidderBid>> result = emxDigitalBidder.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnBannerBidWithCorrectImpId() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder()
                        .imp(singletonList(Imp.builder().id("123").build()))
                        .build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.id("321").impid("123"))));

        // when
        final Result<List<BidderBid>> result = emxDigitalBidder.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder().id("321").impid("123").build(), banner, "USD"));
    }

    @Test
    public void makeBidsShouldReturnVideoBidIfAdmContainsVastPrefix() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder()
                        .imp(singletonList(Imp.builder().id("123").build()))
                        .build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.id("321").adm("<vast data=test").impid("123"))));

        // when
        final Result<List<BidderBid>> result = emxDigitalBidder.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder().id("321").adm("<vast data=test").impid("123").build(),
                        video, "USD"));
    }

    @Test
    public void makeBidsShouldReturnVideoBidIfAdmContainsXmlPrefix() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder()
                        .imp(singletonList(Imp.builder().id("123").build()))
                        .build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.id("321").adm("<?xml data=test").impid("123"))));

        // when
        final Result<List<BidderBid>> result = emxDigitalBidder.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder().id("321").adm("<?xml data=test").impid("123").build(),
                        video, "USD"));
    }

    private static BidResponse givenBidResponse(
            Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer) {
        return BidResponse.builder()
                .cur("USD")
                .seatbid(singletonList(SeatBid.builder()
                        .bid(singletonList(bidCustomizer.apply(Bid.builder()).build()))
                        .build()))
                .build();
    }

    private static BidderCall<BidRequest> givenHttpCall(BidRequest bidRequest, String body) {
        return BidderCall.succeededHttp(
                HttpRequest.<BidRequest>builder().payload(bidRequest).build(),
                HttpResponse.of(200, null, body),
                null);
    }

}

