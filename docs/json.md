구조화된 출력


제공된 JSON 스키마를 준수하는 응답을 생성하도록 Gemini 모델을 구성할 수 있습니다. 이를 통해 예측 가능하고 유형이 안전한 결과를 얻을 수 있으며 비구조화된 텍스트에서 구조화된 데이터를 쉽게 추출할 수 있습니다.

구조화된 출력을 사용하는 것이 적합한 경우는 다음과 같습니다.

데이터 추출: 텍스트에서 이름, 날짜와 같은 특정 정보를 추출합니다.
구조화된 분류: 텍스트를 사전 정의된 카테고리로 분류합니다.
에이전트 워크플로: 도구 또는 API를 위한 구조화된 입력을 생성합니다.
REST API에서 JSON 스키마를 지원하는 것 외에도 Google GenAI SDK를 사용하면 Pydantic (Python) 및 Zod (JavaScript)를 사용하여 스키마를 쉽게 정의할 수 있습니다.

구조화된 출력 예시
레시피 추출기
이 예에서는 object, array, string, integer과 같은 기본 JSON 스키마 유형을 사용하여 텍스트에서 구조화된 데이터를 추출하는 방법을 보여줍니다.

Python
자바스크립트
Go
REST

import { GoogleGenAI } from "@google/genai";
import { z } from "zod";
import { zodToJsonSchema } from "zod-to-json-schema";

const ingredientSchema = z.object({
  name: z.string().describe("Name of the ingredient."),
  quantity: z.string().describe("Quantity of the ingredient, including units."),
});

const recipeSchema = z.object({
  recipe_name: z.string().describe("The name of the recipe."),
  prep_time_minutes: z.number().optional().describe("Optional time in minutes to prepare the recipe."),
  ingredients: z.array(ingredientSchema),
  instructions: z.array(z.string()),
});

const ai = new GoogleGenAI({});

const prompt = `
Please extract the recipe from the following text.
The user wants to make delicious chocolate chip cookies.
They need 2 and 1/4 cups of all-purpose flour, 1 teaspoon of baking soda,
1 teaspoon of salt, 1 cup of unsalted butter (softened), 3/4 cup of granulated sugar,
3/4 cup of packed brown sugar, 1 teaspoon of vanilla extract, and 2 large eggs.
For the best part, they'll need 2 cups of semisweet chocolate chips.
First, preheat the oven to 375°F (190°C). Then, in a small bowl, whisk together the flour,
baking soda, and salt. In a large bowl, cream together the butter, granulated sugar, and brown sugar
until light and fluffy. Beat in the vanilla and eggs, one at a time. Gradually beat in the dry
ingredients until just combined. Finally, stir in the chocolate chips. Drop by rounded tablespoons
onto ungreased baking sheets and bake for 9 to 11 minutes.
`;

const response = await ai.models.generateContent({
  model: "gemini-3.5-flash",
  contents: prompt,
  config: {
    responseFormat: { text: { mimeType: "application/json", schema: zodToJsonSchema(recipeSchema) } },
  },
});

const recipe = recipeSchema.parse(JSON.parse(response.text));
console.log(recipe);
응답 예:


{
  "recipe_name": "Delicious Chocolate Chip Cookies",
  "ingredients": [
    {
      "name": "all-purpose flour",
      "quantity": "2 and 1/4 cups"
    },
    {
      "name": "baking soda",
      "quantity": "1 teaspoon"
    },
    {
      "name": "salt",
      "quantity": "1 teaspoon"
    },
    {
      "name": "unsalted butter (softened)",
      "quantity": "1 cup"
    },
    {
      "name": "granulated sugar",
      "quantity": "3/4 cup"
    },
    {
      "name": "packed brown sugar",
      "quantity": "3/4 cup"
    },
    {
      "name": "vanilla extract",
      "quantity": "1 teaspoon"
    },
    {
      "name": "large eggs",
      "quantity": "2"
    },
    {
      "name": "semisweet chocolate chips",
      "quantity": "2 cups"
    }
  ],
  "instructions": [
    "Preheat the oven to 375°F (190°C).",
    "In a small bowl, whisk together the flour, baking soda, and salt.",
    "In a large bowl, cream together the butter, granulated sugar, and brown sugar until light and fluffy.",
    "Beat in the vanilla and eggs, one at a time.",
    "Gradually beat in the dry ingredients until just combined.",
    "Stir in the chocolate chips.",
    "Drop by rounded tablespoons onto ungreased baking sheets and bake for 9 to 11 minutes."
  ]
}
콘텐츠 검토
이 예에서는 조건부 스키마에 anyOf를, 분류에 enum를 사용하여 콘텐츠에 따라 출력 구조가 달라지도록 합니다.

Python
자바스크립트
Go
REST

import { GoogleGenAI } from "@google/genai";
import { z } from "zod";
import { zodToJsonSchema } from "zod-to-json-schema";

const spamDetailsSchema = z.object({
  reason: z.string().describe("The reason why the content is considered spam."),
  spam_type: z.enum(["phishing", "scam", "unsolicited promotion", "other"]).describe("The type of spam."),
});

const notSpamDetailsSchema = z.object({
  summary: z.string().describe("A brief summary of the content."),
  is_safe: z.boolean().describe("Whether the content is safe for all audiences."),
});

const moderationResultSchema = z.object({
  decision: z.union([spamDetailsSchema, notSpamDetailsSchema]),
});

const ai = new GoogleGenAI({});

const prompt = `
Please moderate the following content and provide a decision.
Content: 'Congratulations! You''ve won a free cruise to the Bahamas. Click here to claim your prize: www.definitely-not-a-scam.com'
`;

const response = await ai.models.generateContent({
  model: "gemini-3.5-flash",
  contents: prompt,
  config: {
    responseFormat: { text: { mimeType: "application/json", schema: zodToJsonSchema(moderationResultSchema) } },
  },
});

const result = moderationResultSchema.parse(JSON.parse(response.text));
console.log(result);
재귀 구조
이 예시에서는 조직도와 같은 재귀 스키마를 정의하는 방법을 보여줍니다.

Python
자바스크립트
Go
REST

import { GoogleGenAI } from "@google/genai";
import { z } from "zod";
import { zodToJsonSchema } from "zod-to-json-schema";

const employeeSchema = z.object({
  name: z.string(),
  employee_id: z.number().int(),
  reports: z.lazy(() => z.array(employeeSchema)).describe("A list of employees reporting to this employee."),
});

const ai = new GoogleGenAI({});

const prompt = `
Generate an organization chart for a small team.
The manager is Alice, who manages Bob and Charlie. Bob manages David.
`;

const response = await ai.models.generateContent({
  model: "gemini-3.5-flash",
  contents: prompt,
  config: {
    responseFormat: { text: { mimeType: "application/json", schema: zodToJsonSchema(employeeSchema) } },
  },
});

const employee = employeeSchema.parse(JSON.parse(response.text));
console.log(employee);
응답 예:


{
  "name": "Alice",
  "employee_id": 101,
  "reports": [
    {
      "name": "Bob",
      "employee_id": 102,
      "reports": [
        {
          "name": "David",
          "employee_id": 104,
          "reports": []
        }
      ]
    },
    {
      "name": "Charlie",
      "employee_id": 103,
      "reports": []
    }
  ]
}
스트리밍
구조화된 출력을 스트리밍할 수 있으므로 전체 출력이 완료될 때까지 기다리지 않고도 대답이 생성되는 대로 처리를 시작할 수 있습니다. 이렇게 하면 애플리케이션의 인식된 성능이 개선될 수 있습니다.

스트리밍된 청크는 유효한 부분 JSON 문자열이며, 이를 연결하여 최종적인 완전한 JSON 객체를 형성할 수 있습니다.

Python
자바스크립트

import { GoogleGenAI } from "@google/genai";
import { z } from "zod";
import { zodToJsonSchema } from "zod-to-json-schema";

const ai = new GoogleGenAI({});
const prompt = "The new UI is incredibly intuitive and visually appealing. Great job! Add a very long summary to test streaming!";

const feedbackSchema = z.object({
  sentiment: z.enum(["positive", "neutral", "negative"]),
  summary: z.string(),
});

const stream = await ai.models.generateContentStream({
  model: "gemini-3.5-flash",
  contents: prompt,
  config: {
    responseFormat: { text: { mimeType: "application/json", schema: zodToJsonSchema(feedbackSchema) } },
  },
});

for await (const chunk of stream) {
  console.log(chunk.candidates[0].content.parts[0].text)
}
도구를 사용한 구조화된 출력
미리보기: 이 기능은 Gemini 3 시리즈 모델인 gemini-3.1-pro-preview 및 gemini-3.5-flash에서만 사용할 수 있습니다.
Gemini 3를 사용하면 Google 검색을 사용한 그라운딩, URL 컨텍스트, 코드 실행, 파일 검색, 함수 호출 등 기본 제공 도구와 구조화된 출력을 결합할 수 있습니다.

Python
자바스크립트
REST

import { GoogleGenAI } from "@google/genai";
import { z } from "zod";
import { zodToJsonSchema } from "zod-to-json-schema";

const ai = new GoogleGenAI({});

const matchSchema = z.object({
  winner: z.string().describe("The name of the winner."),
  final_match_score: z.string().describe("The final score."),
  scorers: z.array(z.string()).describe("The name of the scorer.")
});

async function run() {
  const response = await ai.models.generateContent({
    model: "gemini-3.1-pro-preview",
    contents: "Search for all details for the latest Euro.",
    config: {
      tools: [
        { googleSearch: {} },
        { urlContext: {} }
      ],
      responseFormat: { text: { mimeType: "application/json", schema: zodToJsonSchema(matchSchema) } },
    },
  });

  const match = matchSchema.parse(JSON.parse(response.text));
  console.log(match);
}

run();
JSON 스키마 지원
JSON 객체를 생성하려면 생성 구성에서 response_format을 설정하세요. 스키마는 원하는 출력 형식을 설명하는 유효한 JSON 스키마여야 합니다.

그러면 모델이 제공된 스키마와 일치하는 구문상 유효한 JSON 문자열인 대답을 생성합니다. 구조화된 출력을 사용하면 모델이 스키마의 키와 동일한 순서로 출력을 생성합니다.

Gemini의 구조화된 출력 모드는 JSON 스키마 사양의 일부를 지원합니다.

다음 type 값이 지원됩니다.

string: 텍스트
number: 부동 소수점 숫자의 경우
integer: 정수의 경우
boolean: true/false 값입니다.
object: 키-값 쌍이 있는 구조화된 데이터에 사용됩니다.
array: 항목 목록
null: 속성이 null이 되도록 허용하려면 유형 배열에 "null"를 포함합니다 (예: {"type": ["string", "null"]}).
이러한 설명 속성은 모델을 안내하는 데 도움이 됩니다.

title: 속성에 대한 간단한 설명입니다.
description: 속성에 대한 더 길고 자세한 설명입니다.
유형별 속성
object 값:

properties: 각 키가 속성 이름이고 각 값이 해당 속성의 스키마인 객체입니다.
required: 필수 속성을 나열하는 문자열 배열입니다.
additionalProperties: properties에 나열되지 않은 속성이 허용되는지 여부를 제어합니다. 불리언 또는 스키마일 수 있습니다.
string 값:

enum: 분류 작업에 사용할 수 있는 특정 문자열 집합을 나열합니다.
format: 문자열의 구문을 지정합니다(예: date-time, date, time).
number 및 integer 값:

enum: 가능한 숫자 값의 특정 집합을 나열합니다.
minimum: 최소 포함 값입니다.
maximum: 최대 포함 값입니다.
array 값:

items: 배열의 모든 항목에 대한 스키마를 정의합니다.
prefixItems: 첫 번째 N개 항목의 스키마 목록을 정의하여 튜플과 유사한 구조를 허용합니다.
minItems: 배열의 최소 항목 수입니다.
maxItems: 배열의 최대 항목 수입니다.
모델 지원
다음 모델은 구조화된 출력을 지원합니다.

모델	구조화된 출력
Gemini 3.1 Flash-Lite	✔️
Gemini 3.1 Pro 프리뷰	✔️
Gemini 3.5 Flash	✔️
Gemini 3.1 Flash-Lite 프리뷰	✔️
Gemini 2.5 Pro	✔️
Gemini 2.5 Flash	✔️
Gemini 2.5 Flash-Lite	✔️
Gemini 2.0 Flash	✔️*
Gemini 2.0 Flash-Lite	✔️*
* Gemini 2.0에서는 선호하는 구조를 정의하기 위해 JSON 입력 내에 명시적인 propertyOrdering 목록이 필요합니다. 이 설명서에서 예를 확인할 수 있습니다.

구조화된 출력과 함수 호출 비교
구조화된 출력과 함수 호출은 모두 JSON 스키마를 사용하지만 용도가 다릅니다.

기능	기본 사용 사례
구조화된 출력	사용자에게 최종 대답의 형식을 지정합니다. 모델의 대답이 특정 형식 (예: 데이터베이스에 저장하기 위해 문서에서 데이터 추출)이어야 하는 경우에 사용합니다.
함수 호출	대화 중에 조치를 취합니다. 모델이 최종 답변을 제공하기 전에 사용자에게 작업을 실행하도록 요청해야 하는 경우 (예: '현재 날씨 가져와') 사용합니다.
권장사항
명확한 설명: 스키마의 description 필드를 사용하여 각 속성이 무엇을 나타내는지 모델에 명확하게 설명합니다. 이는 모델의 출력을 안내하는 데 중요합니다.
강한 형식 지정: 가능하면 구체적인 유형 (integer, string, enum)을 사용하세요. 매개변수에 유효한 값의 제한된 집합이 있는 경우 enum를 사용합니다.
프롬프트 엔지니어링: 모델이 수행해야 하는 작업을 프롬프트에 명확하게 명시합니다. 예를 들어 '텍스트에서 다음 정보를 추출해 줘' 또는 '제공된 스키마에 따라 이 의견을 분류해 줘'와 같이 말합니다.
유효성 검사: 구조화된 출력은 구문상 올바른 JSON을 보장하지만 값이 의미상 올바른지는 보장하지 않습니다. 항상 애플리케이션 코드에서 최종 출력을 검증한 후 사용하세요.
오류 처리: 모델의 출력이 스키마를 준수하지만 비즈니스 로직 요구사항을 충족하지 못하는 경우를 원활하게 관리할 수 있도록 애플리케이션에 강력한 오류 처리를 구현합니다.
제한사항
스키마 하위 집합: JSON 스키마 사양의 일부 기능은 지원되지 않습니다. 모델은 지원되지 않는 속성을 무시합니다.
스키마 복잡성: API에서 매우 크거나 깊이 중첩된 스키마를 거부할 수 있습니다. 오류가 발생하면 속성 이름을 단축하거나, 중첩을 줄이거나, 제약 조건 수를 제한하여 스키마를 단순화해 보세요.
도움이 되었나요?