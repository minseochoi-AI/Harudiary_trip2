import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

class DiaryDateDto {
    private String date;
    @SerializedName(value = "plan", alternate = {"isPlan"})
    private Boolean isPlan;

    public String getDate() { return date; }
    public Boolean getIsPlan() { return isPlan; }
}

public class test_gson {
    public static void main(String[] args) {
        String json = "{\"date\":\"2026-06-25\",\"isPlan\":false}";
        Gson gson = new Gson();
        DiaryDateDto dto = gson.fromJson(json, DiaryDateDto.class);
        System.out.println("Parsed isPlan: " + dto.getIsPlan());
    }
}
