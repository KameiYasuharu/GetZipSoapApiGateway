package com.example.apigateway.controller;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.example.apigateway.dto.GetZipResponse;

@Controller
@RequestMapping
public class ApiGatewayController {

	// application.propertiesからAWS API GatewayのURLを注入
	@Value("${aws.api.gateway.url}")
	private String awsApiGatewayUrl;

	// REST API呼び出し用のテンプレート
	@Autowired
	private RestTemplate restTemplate;

	/**
	 * ZIPファイル取得画面を表示する
	 * @return ZIPファイル取得画面のテンプレート名
	 */
	@GetMapping("/getZip_OP8")
	public String showLoginForm() {
		return "zipDownload";
	}

	/**
	 * API Gateway経由でZIPファイルを取得する
	 * @return ZIPファイルを含むレスポンスエンティティ
	 */
	@GetMapping("/ApiGateway_OP8")
	@ResponseBody
	public ResponseEntity<InputStreamResource> zipGateway() {
		try {
			// AWS API Gatewayにリクエストを送信
			GetZipResponse response = restTemplate.getForObject(
					awsApiGatewayUrl,
					GetZipResponse.class);

			// 結果に応じてHTTPステータスを決定
			HttpStatus status = response == null || response.getContent() == null ? HttpStatus.NOT_FOUND
					: HttpStatus.OK;

			// レスポンスチェック
			if (status == HttpStatus.OK) {
				// バイト配列から入力ストリームを作成
				ByteArrayInputStream bis = new ByteArrayInputStream(response.getContent());
				InputStreamResource resource = new InputStreamResource(bis);

				// ファイル名をUTF-8でエンコード（スペースを%20に置換）
				String encodedFilename = URLEncoder.encode("web.zip", "UTF-8").replace("+", "%20");

				// レスポンスヘッダーを設定
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // バイナリデータのコンテンツタイプ
				headers.setContentDisposition(
						ContentDisposition.attachment() // 添付ファイルとしてダウンロード
								.filename(encodedFilename) // エンコード済みファイル名
								.build());

				// 成功レスポンスを返す
				return ResponseEntity.ok()
						.headers(headers)
						.body(resource);
			} else {
				// ZIPファイルが存在しない場合のエラーメッセージ
				String errorMessage = "ZIPファイルが存在しない";
				ByteArrayInputStream errorStream = new ByteArrayInputStream(
						errorMessage.getBytes(StandardCharsets.UTF_8));
				InputStreamResource resource = new InputStreamResource(errorStream);

				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.contentType(MediaType.TEXT_PLAIN) // プレーンテキストとして返す
						.body(resource);
			}
		} catch (Exception e) {
			// 例外発生時の処理
			return ResponseEntity.internalServerError().body(
					new InputStreamResource(new ByteArrayInputStream(e.getMessage().getBytes(StandardCharsets.UTF_8)))); // エラーメッセージを返す
		}
	}
}

