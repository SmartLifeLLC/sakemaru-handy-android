# Claude Code 작업지시서
## 配送コース選択 화면 — UI 및 색상 변경
### 첨부 이미지 기준 리디자인

---

## 🎯 작업 목표

| 항목 | 내용 |
|------|------|
| **대상 화면** | 配送コース選択 (배송 코스 선택) 화면 |
| **레퍼런스** | 첨부 스크린샷 이미지 |
| **작업 내용** | 헤더 디자인, 화면 배경, 카드 레이아웃·색상, 상태별 카드 색상 변경 |
| **유지할 것** | 기존 로직·API·ViewModel·네비게이션 — **일절 수정 금지** |
| **변경할 것** | UI 표현만 (Layout / Composable) |

---

## 📂 Step 0 — 현재 화면 파악 (착수 전 필수)

```bash
find sakemaru-handy-denso -type f -name "*.kt" | \
  xargs grep -l "Course\|course\|配送\|コース" 2>/dev/null

find sakemaru-handy-denso -type f -name "*.xml" | \
  xargs grep -l "course\|Course" 2>/dev/null
```

### 파악 결과 메모

```
[UI 기술]  Compose / XML  ← 확인 후 기재

[파일 목록]
- UI 파일          : <파일 경로>
- ViewModel        : <파일 경로>  ← 수정 안 함

[카드 상태값 확인]
- 상태를 나타내는 Enum / sealed class 이름  : <확인>
- 미착수 값  : <확인>
- 착수중 값  : <확인>
- 착수완료 값: <확인>

[카드 데이터 모델 필드 확인]
- 코스명 필드명      : <확인>
- 에리어 설명 필드명 : <확인>
- 出荷指示 건수 필드명: <확인>
- 検品済 건수 필드명 : <확인>
- 상태 필드명        : <확인>
```

---

## 🎨 Step 1 — 전체 화면 배경

```
배경색 : #FDFBF2  (크림색 / 연한 베이지)
```

---

## 🎨 Step 2 — 헤더 디자인

```
배경색   : #FFFFFF  (또는 투명 — 배경색이 비치도록)
높이     : 56dp
하단 테두리 : 없음 (기존 테두리가 있으면 제거)
```

### 헤더 내부 레이아웃 (좌 → 우)

```
[←]  [🚚 아이콘]  [배송코ース선택]  [│]  [창고명]

① 뒤로가기 아이콘 (←)
   색상    : #C0392B (주황빛 적색)
   크기    : 24dp
   좌측 여백: 12dp

② 트럭 아이콘 (🚚)
   색상    : #E67E22 (주황)
   크기    : 22dp
   좌측 여백: 8dp

③ 화면 타이틀 텍스트  「配送コース選択」
   폰트    : 18sp / Bold
   색상    : #C0392B (주황빛 적색)
   좌측 여백: 6dp

④ 구분선  「│」
   색상    : #CCCCCC
   좌우 여백: 10dp

⑤ 창고명 (ViewModel 에서 동적)
   폰트    : 16sp / 일반 (Regular)
   색상    : #E67E22 (주황)
```

---

## 🎨 Step 3 — 안내 문구 영역

헤더 아래, 카드 리스트 위.

```
텍스트   : 기존 안내 문구 유지
폰트     : 14sp / 일반
색상     : #555555
상단 여백 : 12dp
좌측 여백 : 16dp
하단 여백 : 12dp
```

---

## 🎨 Step 4 — 카드 그리드 레이아웃

```
열 수    : 2열  (좌·우 동일 너비)
카드 간격 : 12dp (수직 · 수평 동일)
좌우 패딩 : 16dp
상단 패딩 : 0dp
```

> 카드 수가 홀수일 경우(예: 5개) 마지막 카드는 좌측 열에 단독 배치, 우측은 빈칸.

---

## 🎨 Step 5 — 카드 공통 사양

```
corner radius  : 16dp
테두리 두께    : 2dp
패딩           : 16dp (전체)
최소 높이      : 120dp
그림자         : elevation 2dp

내부 레이아웃 (세로 배치):
  [1행] 🚚 아이콘 + 코스명  (가로 배치)
  [2행] 에리어 설명 텍스트
  [3행] 「出荷指示: X件　検品済: X件」
```

### 카드 내부 텍스트 사양

```
[코스명]
  폰트   : 16sp / Bold
  색상   : 상태별 (아래 Step 6 참고)
  아이콘 : 🚚  16sp  코스명과 8dp 간격

[에리어 설명]
  폰트   : 13sp / 일반
  색상   : #555555
  상단 여백: 6dp

[건수 텍스트]  「出荷指示: X件　検品済: X件」
  폰트   : 13sp / 일반
  색상   : #555555
  상단 여백: 6dp
```

---

## 🎨 Step 6 — 상태별 카드 색상

### 미착수 (未着手)

```
배경색      : #FFFDE7  (연한 노랑)
테두리 색   : #F9A825  (주황 노랑)
코스명 색   : #E67E22  (주황)
아이콘 색   : #E67E22

hover / press:
  배경색    : #FFF9C4  (조금 짙은 노랑)
  테두리 색 : #F57F17
```

### 착수중 (着手中)

```
배경색      : #E8F5E9  (연한 초록)
테두리 색   : #4CAF50  (초록)
코스명 색   : #2E7D32  (짙은 초록)
아이콘 색   : #2E7D32

hover / press:
  배경색    : #C8E6C9  (조금 짙은 초록)
  테두리 색 : #388E3C
```

### 착수완료 (着手完了)

```
배경색      : #F5F5F5  (연한 회색)
테두리 색   : #BDBDBD  (회색)
코스명 색   : #757575  (중간 회색)
아이콘 색   : #757575

hover / press:
  배경색    : #EEEEEE  (조금 짙은 회색)
  테두리 색 : #9E9E9E
```

---

## 🛠 Step 7 — 구현 참고

### Compose — 상태별 색상 함수

```kotlin
data class CourseCardColors(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val backgroundHover: Color,
    val borderHover: Color
)

fun courseCardColors(status: CourseStatus): CourseCardColors = when (status) {
    CourseStatus.NOT_STARTED -> CourseCardColors(
        background     = Color(0xFFFFFDE7),
        border         = Color(0xFFF9A825),
        titleColor     = Color(0xFFE67E22),
        backgroundHover= Color(0xFFFFF9C4),
        borderHover    = Color(0xFFF57F17)
    )
    CourseStatus.IN_PROGRESS -> CourseCardColors(
        background     = Color(0xFFE8F5E9),
        border         = Color(0xFF4CAF50),
        titleColor     = Color(0xFF2E7D32),
        backgroundHover= Color(0xFFC8E6C9),
        borderHover    = Color(0xFF388E3C)
    )
    CourseStatus.COMPLETED -> CourseCardColors(
        background     = Color(0xFFF5F5F5),
        border         = Color(0xFFBDBDBD),
        titleColor     = Color(0xFF757575),
        backgroundHover= Color(0xFFEEEEEE),
        borderHover    = Color(0xFF9E9E9E)
    )
}
```

> `CourseStatus` Enum 명칭은 Step 0 에서 확인한 실제 값으로 교체할 것.

### hover / press 효과 적용

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val colors = courseCardColors(course.status)

Card(
    colors = CardDefaults.cardColors(
        containerColor = if (isPressed) colors.backgroundHover else colors.background
    ),
    border = BorderStroke(
        2.dp,
        if (isPressed) colors.borderHover else colors.border
    ),
    modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .clickable(interactionSource = interactionSource, indication = null) {
            onCourseSelected(course)  // 기존 로직 연결 유지
        }
)
```

### XML — 상태별 selector drawable

각 상태마다 `res/drawable/` 에 selector 파일 생성:

```xml
<!-- bg_card_not_started.xml -->
<selector>
    <item android:state_pressed="true">
        <shape>
            <solid android:color="#FFF9C4"/>
            <stroke android:width="2dp" android:color="#F57F17"/>
            <corners android:radius="16dp"/>
        </shape>
    </item>
    <item>
        <shape>
            <solid android:color="#FFFDE7"/>
            <stroke android:width="2dp" android:color="#F9A825"/>
            <corners android:radius="16dp"/>
        </shape>
    </item>
</selector>
```

> 착수중 / 착수완료도 동일 패턴으로 색상만 바꿔 생성.

---

## 🔒 Step 8 — 수정 금지 항목

```
□ 카드 탭 시 코스 선택 처리 로직
□ ViewModel 에서 코스 목록 로드 로직
□ 각 코스의 상태 판단 로직
□ 네비게이션 (뒤로가기, 코스 선택 후 다음 화면 이동)
□ 건수 데이터 (出荷指示, 検品済) 취득 로직
□ 창고명 표시 데이터 취득 로직
```

---

## ✅ Step 9 — 완료 체크리스트

### 화면 전체

- [ ] 배경색이 크림/연베이지 (`#FDFBF2`) 로 변경됨

### 헤더

- [ ] 트럭 아이콘 + 타이틀 「配送コース選択」주황적색으로 표시
- [ ] 구분선 「│」 이후 창고명이 주황색으로 표시
- [ ] 뒤로가기 아이콘 색상 적용

### 카드 레이아웃

- [ ] 2열 그리드 배치
- [ ] 홀수 카드 수일 때 마지막 카드가 좌측에 단독 배치
- [ ] 카드 간격 12dp

### 미착수 카드

- [ ] 배경 `#FFFDE7` / 테두리 `#F9A825`
- [ ] 코스명 · 아이콘 색상 `#E67E22`
- [ ] press 시 배경 `#FFF9C4` / 테두리 `#F57F17`

### 착수중 카드

- [ ] 배경 `#E8F5E9` / 테두리 `#4CAF50`
- [ ] 코스명 · 아이콘 색상 `#2E7D32`
- [ ] press 시 배경 `#C8E6C9` / 테두리 `#388E3C`

### 착수완료 카드

- [ ] 배경 `#F5F5F5` / 테두리 `#BDBDBD`
- [ ] 코스명 · 아이콘 색상 `#757575`
- [ ] press 시 배경 `#EEEEEE` / 테두리 `#9E9E9E`

### 카드 내부 텍스트

- [ ] 코스명 16sp Bold / 에리어 설명 13sp / 건수 13sp
- [ ] 건수가 ViewModel 값으로 동적 표시

### 기능 유지

- [ ] 카드 탭 → 기존 선택 처리 정상 동작
- [ ] 뒤로가기 → 정상 동작
- [ ] 빌드 에러 없음

---

*수정 대상은 UI 파일뿐입니다. ViewModel 이하 레이어는 열어보기만 하고 저장하지 마세요.*