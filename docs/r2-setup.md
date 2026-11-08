# Cloudflare R2 설정

이미지 업로드(API 명세서 2-9)에 필요한 버킷 설정과 배포 전 확인 절차다. 코드가 아니라 Cloudflare 대시보드에서 한다.

## 1. 버킷과 API 토큰

1. R2 버킷을 만들고 공개 개발 URL(`https://{bucket-id}.r2.dev`)을 켠다.
2. 이 버킷에 대한 **Object Read & Write** 권한의 R2 API 토큰을 만든다. 재검증 실패 시 객체를 지우므로 쓰기 권한이 필요하다.
3. 백엔드 환경변수를 등록한다. 하나라도 없으면 운영 기동이 실패한다.

| 이름 | 값 |
| --- | --- |
| `R2_ACCOUNT_ID` | Cloudflare 계정 ID |
| `R2_ACCESS_KEY` · `R2_SECRET_KEY` | API 토큰의 Access Key ID · Secret Access Key |
| `R2_BUCKET` | 버킷 이름 |
| `R2_PUBLIC_BASE_URL` | `https://{bucket-id}.r2.dev` |

## 2. 버킷 CORS

브라우저가 발급받은 주소로 R2에 직접 `PUT`하므로 버킷 CORS가 필요하다. 버킷 **Settings → CORS Policy**에 아래 값을 넣는다. `{vercel-app}`은 실제 배포 도메인으로 바꾸고, 로컬 프론트에서 시험하지 않는다면 `localhost` 항목은 뺀다.

```json
[
  {
    "AllowedOrigins": ["https://{vercel-app}.vercel.app", "http://localhost:3000"],
    "AllowedMethods": ["PUT"],
    "AllowedHeaders": ["Content-Type"],
    "MaxAgeSeconds": 3600
  }
]
```

## 3. 실제 버킷 확인 (Phase 4 완료 기준)

작게 신고하고 실제로는 5MB를 넘는 파일을 올렸을 때 저장이 막히는지 확인한다. 자동 테스트(`R2ClientCompatibilityTest`)는 S3 호환 서버로 같은 흐름을 검증하지만, R2 자체와 버킷 CORS는 여기서만 확인된다.

1. `POST /api/admin/uploads/presign`에 `{ "target": "project", "contentType": "image/webp", "size": 1024 }`를 보낸다.
2. 응답의 `uploadUrl`로 6MB 파일을 올린다. `200`이 정상이다 — 발급 주소는 실제 크기를 제한하지 않는다.

   ```bash
   curl -X PUT -H "Content-Type: image/webp" --data-binary @six-mb.webp "<uploadUrl>"
   ```

3. `publicUrl`을 `thumbnailUrl`로 넣어 `POST /api/admin/projects`를 보낸다.
4. `400 UPLOAD_VERIFICATION_FAILED`와 `fieldErrors[thumbnailUrl]`이 오고, 버킷에서 객체가 사라졌는지 확인한다.
5. 배포된 프론트에서 정상 이미지를 올려 브라우저 콘솔에 CORS 오류가 없는지 확인한다.
