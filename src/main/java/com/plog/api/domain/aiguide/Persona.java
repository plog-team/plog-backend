package com.plog.api.domain.aiguide;

/**
 * 일기 초안 작성 시 적용되는 톤·문체 페르소나 5종.
 * 각 페르소나의 systemPromptFragment는 Gemini system 영역에 직접 들어감.
 */
public enum Persona {

    DEFAULT(
        "기본",
        "담백한 일상체로 사실 위주로 짧고 깔끔하게 쓰세요. " +
        "'따스한 햇살 아래' 같은 문학적·시적 수사 절대 금지. 형용사 남용 금지."
    ),

    FRIENDLY(
        "친절함",
        "다정한 친구가 옆에서 듣고 말해주듯 따뜻한 톤. " +
        "'~네요', '~죠?' 같은 부드러운 종결과 가벼운 격려 한 마디 포함. 시적 표현은 자제."
    ),

    EMOTIONAL(
        "감성적",
        "감정 형용사와 비유를 적당히 써서 사진 속 분위기·기분을 풍부하게 묘사. " +
        "다만 클리셰('따스한 햇살', '평온한 한때')는 피하고, 사용자가 실제 답한 디테일에서 감정을 풀어내세요."
    ),

    FORMAL(
        "딱딱함",
        "보고서 톤의 격식체. 종결어미는 모두 '~다'. 사실 위주, 감정은 1~2문장만 짧게. " +
        "'~요/~죠' 같은 비격식체 절대 금지."
    ),

    WITTY(
        "위트",
        "짧고 솔직한 문장에 가벼운 자조나 농담 한두 줄 섞기. 격식 X. " +
        "이모지는 안 쓰되, 친구한테 카톡 보내듯 자연스러운 톤."
    );

    private final String koreanName;
    private final String systemPromptFragment;

    Persona(String koreanName, String systemPromptFragment) {
        this.koreanName = koreanName;
        this.systemPromptFragment = systemPromptFragment;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getSystemPromptFragment() {
        return systemPromptFragment;
    }

    public static Persona orDefault(Persona p) {
        return p == null ? DEFAULT : p;
    }
}
