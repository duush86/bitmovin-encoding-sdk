import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.AclEntry;
import com.bitmovin.api.sdk.model.AclPermission;
import com.bitmovin.api.sdk.model.CodecConfiguration;
import com.bitmovin.api.sdk.model.DashManifest;
import com.bitmovin.api.sdk.model.DashManifestDefault;
import com.bitmovin.api.sdk.model.DashManifestDefaultVersion;
import com.bitmovin.api.sdk.model.Encoding;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.Thumbnail;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.HlsManifest;
import com.bitmovin.api.sdk.model.HlsManifestDefault;
import com.bitmovin.api.sdk.model.HlsManifestDefaultVersion;
import com.bitmovin.api.sdk.model.HttpInput;
import com.bitmovin.api.sdk.model.Input;
import com.bitmovin.api.sdk.model.MessageType;
import com.bitmovin.api.sdk.model.MuxingStream;
import com.bitmovin.api.sdk.model.Output;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.S3Output;
import com.bitmovin.api.sdk.model.S3Input;
import com.bitmovin.api.sdk.model.StartEncodingRequest;
import com.bitmovin.api.sdk.model.Status;
import com.bitmovin.api.sdk.model.Stream;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamMode;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import com.bitmovin.api.sdk.model.Task;
import common.ConfigProvider;
import feign.Logger.Level;
import feign.slf4j.Slf4jLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example demonstrates how to create default DASH and HLS manifests for an encoding.
 *
 * <p>The following configuration parameters are expected:
 *
 * <ul>
 *   <li>BITMOVIN_API_KEY - Your API key for the Bitmovin API
 *   <li>HTTP_INPUT_HOST - The Hostname or IP address of the HTTP server hosting your input files,
 *       e.g.: my-storage.biz
 *   <li>HTTP_INPUT_FILE_PATH - The path to your input file on the provided HTTP server Example:
 *       videos/1080p_Sintel.mp4
 *   <li>S3_OUTPUT_BUCKET_NAME - The name of your S3 output bucket. Example: my-bucket-name
 *   <li>S3_OUTPUT_ACCESS_KEY - The access key of your S3 output bucket
 *   <li>S3_OUTPUT_SECRET_KEY - The secret key of your S3 output bucket
 *   <li>S3_OUTPUT_BASE_PATH - The base path on your S3 output bucket where content will be written.
 *       Example: /outputs
 * </ul>
 *
 * <p>Configuration parameters will be retrieved from these sources in the listed order:
 *
 * <ol>
 *   <li>command line arguments (eg BITMOVIN_API_KEY=xyz)
 *   <li>properties file located in the root folder of the JAVA examples at ./examples.properties
 *       (see examples.properties.template as reference)
 *   <li>environment variables
 *   <li>properties file located in the home folder at ~/.bitmovin/examples.properties (see
 *       examples.properties.template as reference)
 * </ol>
 */
public class DefaultManifests {

  private static final Logger logger = LoggerFactory.getLogger(DefaultManifests.class);

  private static BitmovinApi bitmovinApi;
  private static ConfigProvider configProvider;

  public static void main(String[] args) throws Exception {
    //configProvider = new ConfigProvider(args);
    bitmovinApi =
        BitmovinApi.builder()
            .withApiKey("")
            .withLogger(
                new Slf4jLogger(), Level.BASIC) // set the logger and log level for the API client
            .build();

    Encoding encoding =
        createEncoding(
            "Encoding with default manifests", "Encoding with HLS and DASH default manifests");

//    Input input = createHttpInput(configProvider.getHttpInputHost());
//    Output output =
//        createS3Output(
//            configProvider.getS3OutputBucketName(),
//            configProvider.getS3OutputAccessKey(),
//            configProvider.getS3OutputSecretKey());
    
    S3Input input = bitmovinApi.encoding.inputs.s3.get("5f76fd83-6d16-4bab-8069-db6450686778");
    
    S3Output output =  bitmovinApi.encoding.outputs.s3.get("44f70a74-392f-4e1c-8557-697ef6922410");


    String inputFilePath = "/Earth.mp4";

    // Add a template video stream to the encoding
    //H264VideoConfiguration h264Config = createH264VideoConfig();
  final List<H264VideoConfiguration> videoConfigurations =
  Arrays.asList(
      createH264VideoConfig(1080, 4_800_000L),
      createH264VideoConfig(720, 2_400_000L),
      createH264VideoConfig(480, 1_200_000L));
  
  for (H264VideoConfiguration videoConfiguration : videoConfigurations) {

	  	Stream videoStream =
        createStream(encoding, input, inputFilePath, videoConfiguration);
    	createFmp4Muxing(encoding, output, "video", videoStream);
  }

    // Add audio stream to the encoding
    AacAudioConfiguration aacConfig = createAacAudioConfig();
    Stream audioStream =
        createStream(encoding, input, inputFilePath, aacConfig);
    createFmp4Muxing(encoding, output, "audio", audioStream);

    executeEncoding(encoding);

    generateDashManifest(encoding, output, "/");
    generateHlsManifest(encoding, output, "/");
  }

  /**
   * Creates a resource representing an HTTP server providing the input files. For alternative input
   * methods see <a
   * href="https://bitmovin.com/docs/encoding/articles/supported-input-output-storages">list of
   * supported input and output storages</a>
   *
   * <p>For reasons of simplicity, a new input resource is created on each execution of this
   * example. In production use, this method should be replaced by a <a
   * href="https://bitmovin.com/docs/encoding/api-reference/sections/inputs#/Encoding/GetEncodingInputsHttpByInputId">get
   * call</a> to retrieve an existing resource.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/inputs#/Encoding/PostEncodingInputsHttp
   *
   * @param host The hostname or IP address of the HTTP server e.g.: my-storage.biz
   */
//  private static HttpInput createHttpInput(String host) throws BitmovinException {
//    HttpInput input = new HttpInput();
//    input.setHost(host);
//
//    return bitmovinApi.encoding.inputs.http.create(input);
//  }

  /**
   * Creates a resource representing an AWS S3 cloud storage bucket to which generated content will
   * be transferred. For alternative output methods see <a
   * href="https://bitmovin.com/docs/encoding/articles/supported-input-output-storages">list of
   * supported input and output storages</a>
   *
   * <p>The provided credentials need to allow <i>read</i>, <i>write</i> and <i>list</i> operations.
   * <i>delete</i> should also be granted to allow overwriting of existings files. See <a
   * href="https://bitmovin.com/docs/encoding/faqs/how-do-i-create-a-aws-s3-bucket-which-can-be-used-as-output-location">creating
   * an S3 bucket and setting permissions</a> for further information
   *
   * <p>For reasons of simplicity, a new output resource is created on each execution of this
   * example. In production use, this method should be replaced by a <a
   * href="https://bitmovin.com/docs/encoding/api-reference/sections/outputs#/Encoding/GetEncodingOutputsS3">get
   * call</a> retrieving an existing resource.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/outputs#/Encoding/PostEncodingOutputsS3
   *
   * @param bucketName The name of the S3 bucket
   * @param accessKey The access key of your S3 account
   * @param secretKey The secret key of your S3 account
   */
//  private static S3Output createS3Output(String bucketName, String accessKey, String secretKey)
//      throws BitmovinException {
//
//    S3Output s3Output = new S3Output();
//    s3Output.setBucketName(bucketName);
//    s3Output.setAccessKey(accessKey);
//    s3Output.setSecretKey(secretKey);
//
//    return bitmovinApi.encoding.outputs.s3.create(s3Output);
//  }

  /**
   * Creates an Encoding object. This is the base object to configure your encoding.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodings
   *
   * @param name This is the name of the encoding
   * @param description This is the description of the encoding
   */
  private static Encoding createEncoding(String name, String description) throws BitmovinException {
    Encoding encoding = new Encoding();
    encoding.setName(name);
    encoding.setDescription(description);

    return bitmovinApi.encoding.encodings.create(encoding);
  }

  /**
   * Create a stream which binds an input file to a codec configuration. The stream is used later
   * for muxings.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodingsStreamsByEncodingId
   *
   * @param encoding The encoding where to add the stream to
   * @param input The input where the input file is located
   * @param inputPath The path to the input file
   * @param codecConfiguration The codec configuration to be applied to the stream
   */
  private static Stream createStream(
      Encoding encoding, Input input, String inputPath, CodecConfiguration codecConfiguration)
      throws BitmovinException {
    StreamInput streamInput = new StreamInput();
    streamInput.setInputId(input.getId());
    streamInput.setInputPath(inputPath);
    streamInput.setSelectionMode(StreamSelectionMode.AUTO);

    Stream stream = new Stream();
    stream.addInputStreamsItem(streamInput);
    stream.setCodecConfigId(codecConfiguration.getId());
    stream.setMode(StreamMode.STANDARD);

    return bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream);
  }

  /**
   * Creates a configuration for the H.264 video codec to be applied to video streams.
   *
   * <p>The output resolution is defined by setting the height to 1080 pixels. Width will be
   * determined automatically to maintain the aspect ratio of your input video.
   *
   * <p>To keep things simple, we use a quality-optimized VoD preset configuration, which will apply
   * proven settings for the codec. See <a
   * href="https://bitmovin.com/docs/encoding/tutorials/how-to-optimize-your-h264-codec-configuration-for-different-use-cases">How
   * to optimize your H264 codec configuration for different use-cases</a> for alternative presets.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/configurations#/Encoding/PostEncodingConfigurationsVideoH264
   */
  private static H264VideoConfiguration createH264VideoConfig(int height,long bitrate) throws BitmovinException {
    
	  H264VideoConfiguration config = new H264VideoConfiguration();
    config.setName("H.264 1080p 1.5 Mbit/s");
    config.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);
    config.setHeight(height);
    config.setBitrate(bitrate);
    
    
//    final List<H264VideoConfiguration> videoConfigurations =
//            Arrays.asList(
//                createH264VideoConfig(1080, 4_800_000L),
//                createH264VideoConfig(720, 2_400_000L),
//                createH264VideoConfig(480, 1_200_000L));

    return bitmovinApi.encoding.configurations.video.h264.create(config);
  }

  /**
   * Creates a configuration for the AAC audio codec to be applied to audio streams.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/configurations#/Encoding/PostEncodingConfigurationsAudioAac
   */
  private static AacAudioConfiguration createAacAudioConfig() throws BitmovinException {
    AacAudioConfiguration config = new AacAudioConfiguration();
    config.setName("AAC 128 kbit/s");
    config.setBitrate(128_000L);

    return bitmovinApi.encoding.configurations.audio.aac.create(config);
  }

  /**
   * Creates a fragmented MP4 muxing. This will generate segments with a given segment length for
   * adaptive streaming.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/all#/Encoding/PostEncodingEncodingsMuxingsFmp4ByEncodingId
   *
   * @param encoding The encoding where to add the muxing to
   * @param output The output that should be used for the muxing to write the segments to
   * @param outputPath The output path where the fragmented segments will be written to
   * @param stream The stream to be muxed
   */
  private static Fmp4Muxing createFmp4Muxing(
      Encoding encoding, Output output, String outputPath, Stream stream) throws BitmovinException {
    MuxingStream muxingStream = new MuxingStream();
    muxingStream.setStreamId(stream.getId());
    
    

    Fmp4Muxing muxing = new Fmp4Muxing();
    muxing.addOutputsItem(buildEncodingOutput(output, outputPath));
    muxing.addStreamsItem(muxingStream);
    muxing.setSegmentLength(4.0);
    
    Thumbnail tb = new Thumbnail();
    
    
    
    return bitmovinApi.encoding.encodings.muxings.fmp4.create(encoding.getId(), muxing);
  }

  /**
   * Builds an EncodingOutput object which defines where the output content (e.g. of a muxing) will
   * be written to. Public read permissions will be set for the files written, so they can be
   * accessed easily via HTTP.
   *
   * @param output The output resource to be used by the EncodingOutput
   * @param outputPath The path where the content will be written to
   */
  private static EncodingOutput buildEncodingOutput(Output output, String outputPath) {
    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);

    EncodingOutput encodingOutput = new EncodingOutput();
    encodingOutput.setOutputPath(buildAbsolutePath(outputPath));
    encodingOutput.setOutputId(output.getId());
    encodingOutput.addAclItem(aclEntry);
    return encodingOutput;
  }

  /**
   * Builds an absolute path by concatenating the S3_OUTPUT_BASE_PATH configuration parameter, the
   * name of this example class and the given relative path
   *
   * <p>e.g.: /s3/base/path/ClassName/relative/path
   *
   * @param relativePath The relative path that is concatenated
   * @return The absolute path
   */
  public static String buildAbsolutePath(String relativePath) {
    //String className = DefaultManifests.class.getSimpleName();
	//long now = Instant.now().toEpochMilli();
	
	//byte[] array = new byte[7]; // length is bounded by 7
    //new Random().nextBytes(array);
   // String generatedString = new String(array, Charset.forName("UTF-8"));
    
    //String videoIdString = new String(Long.toString(now));
  
    return Paths.get("outputs", "earthvideo" , relativePath).toString();
  }

  /**
   * Starts the actual encoding process and periodically polls its status until it reaches a final
   * state
   *
   * <p>API endpoints:
   * https://bitmovin.com/docs/encoding/api-reference/all#/Encoding/PostEncodingEncodingsStartByEncodingId
   * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/GetEncodingEncodingsStatusByEncodingId
   *
   * <p>Please note that you can also use our webhooks API instead of polling the status. For more
   * information consult the API spec:
   * https://bitmovin.com/docs/encoding/api-reference/sections/notifications-webhooks
   *
   * @param encoding The encoding to be started
   */
  private static void executeEncoding(Encoding encoding)
      throws InterruptedException, BitmovinException {
    
	  
	  bitmovinApi.encoding.encodings.start(encoding.getId(), new StartEncodingRequest());
	  //Thumbnail tb = Thumbnail("");

    Task task;
    do {
      Thread.sleep(5000);
      task = bitmovinApi.encoding.encodings.status(encoding.getId());
      logger.info("encoding status is {} (progress: {} %)", task.getStatus(), task.getProgress());
    } while (task.getStatus() != Status.FINISHED && task.getStatus() != Status.ERROR);

    if (task.getStatus() == Status.ERROR) {
      logTaskErrors(task);
      throw new RuntimeException("Encoding failed");
    }
    logger.info("encoding finished successfully");
  }

  /**
   * Creates an HLS default manifest that automatically includes all representations configured in
   * the encoding.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsHlsDefault
   *
   * @param encoding The encoding for which the manifest should be generated
   * @param output The output to which the manifest should be written
   * @param outputPath The path to which the manifest should be written
   */
  private static void generateHlsManifest(Encoding encoding, Output output, String outputPath)
      throws Exception {
    HlsManifestDefault hlsManifestDefault = new HlsManifestDefault();
    hlsManifestDefault.setEncodingId(encoding.getId());
    hlsManifestDefault.addOutputsItem(buildEncodingOutput(output, outputPath));
    hlsManifestDefault.setName("master.m3u8");
    hlsManifestDefault.setVersion(HlsManifestDefaultVersion.V1);

    hlsManifestDefault = bitmovinApi.encoding.manifests.hls.defaultapi.create(hlsManifestDefault);
    executeHlsManifestCreation(hlsManifestDefault);
  }

  /**
   * Creates a DASH default manifest that automatically includes all representations configured in
   * the encoding.
   *
   * <p>API endpoint:
   * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsDash
   *
   * @param encoding The encoding for which the manifest should be generated
   * @param output The output where the manifest should be written to
   * @param outputPath The path to which the manifest should be written
   */
  private static void generateDashManifest(Encoding encoding, Output output, String outputPath)
      throws Exception {
    DashManifestDefault dashManifestDefault = new DashManifestDefault();
    dashManifestDefault.setEncodingId(encoding.getId());
    dashManifestDefault.setManifestName("stream.mpd");
    dashManifestDefault.setVersion(DashManifestDefaultVersion.V1);
    dashManifestDefault.addOutputsItem(buildEncodingOutput(output, outputPath));
    dashManifestDefault =
        bitmovinApi.encoding.manifests.dash.defaultapi.create(dashManifestDefault);
    executeDashManifestCreation(dashManifestDefault);
  }

  /**
   * Starts the DASH manifest creation and periodically polls its status until it reaches a final
   * state
   *
   * <p>API endpoints:
   * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsDashStartByManifestId
   * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/GetEncodingManifestsDashStatusByManifestId
   *
   * @param dashManifest The DASH manifest to be created
   */
  private static void executeDashManifestCreation(DashManifest dashManifest)
      throws BitmovinException, InterruptedException {
    bitmovinApi.encoding.manifests.dash.start(dashManifest.getId());

    Task task;
    do {
      Thread.sleep(1000);
      task = bitmovinApi.encoding.manifests.dash.status(dashManifest.getId());
    } while (task.getStatus() != Status.FINISHED && task.getStatus() != Status.ERROR);

    if (task.getStatus() == Status.ERROR) {
      logTaskErrors(task);
      throw new RuntimeException("DASH manifest creation failed");
    }
    logger.info("DASH manifest creation finished successfully");
  }

  /**
   * Starts the HLS manifest creation and periodically polls its status until it reaches a final
   * state
   *
   * <p>API endpoints:
   * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsHlsStartByManifestId
   * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/GetEncodingManifestsHlsStatusByManifestId
   *
   * @param hlsManifest The HLS manifest to be created
   */
  private static void executeHlsManifestCreation(HlsManifest hlsManifest)
      throws BitmovinException, InterruptedException {

    bitmovinApi.encoding.manifests.hls.start(hlsManifest.getId());

    Task task;
    do {
      Thread.sleep(1000);
      task = bitmovinApi.encoding.manifests.hls.status(hlsManifest.getId());
    } while (task.getStatus() != Status.FINISHED && task.getStatus() != Status.ERROR);

    if (task.getStatus() == Status.ERROR) {
      logTaskErrors(task);
      throw new RuntimeException("HLS manifest creation failed");
    }
    logger.info("HLS manifest creation finished successfully");
  }

  private static void logTaskErrors(Task task) {
    task.getMessages().stream()
        .filter(msg -> msg.getType() == MessageType.ERROR)
        .forEach(msg -> logger.error(msg.getText()));
  }
}
