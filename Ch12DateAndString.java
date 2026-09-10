import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 택배의 현재 상태와 배송 이력에 기록할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

public class Ch12DateAndString {
    // 날짜와 문자열 리팩토링 결과를 실행해 보는 예시다.
    public static void main(String[] args) {
        ParcelService parcelService = new ParcelService(new MemoryParcelRepository());

        try {
            parcelService.register("1001", "홍길동", "010-1111-2222", "제주", 3, "특급");
            parcelService.changeStatus("1001", ParcelStatus.출고);
            System.out.println(parcelService.getDetail("1001"));
            System.out.println(parcelService.getHistoryText("1001"));
        } catch (ParcelException exception) {
            System.out.println(exception.getMessage());
        }
    }
}

// 택배 업무와 날짜 계산을 처리하는 클래스다.
class ParcelService {
    private ParcelRepository parcelRepository;

    // 업무 처리에 사용할 저장소를 받는다.
    ParcelService(ParcelRepository parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    // ========== CH12 변경 ==========
    // 현재 날짜로 접수일과 예상 도착일을 계산해 택배를 접수한다.
    // =================================
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType) throws ParcelException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        LocalDate registeredDate = LocalDate.now();
        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, registeredDate);
        parcel.expectedDeliveryDate = registeredDate.plusDays(parcel.getExpectedDeliveryDays());
        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수);
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, ParcelStatus afterStatus) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.status != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.status, afterStatus);
        parcel.status = afterStatus;
        parcelRepository.save(parcel);
    }

    // 운송장 번호에 해당하는 택배를 문자열로 정리해 반환한다.
    String getDetail(String trackingNumber) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);

        // ========== CH12 변경 ==========
        // 반복되는 문자열 연결에는 StringBuilder를 사용한다.
        // =================================
        StringBuilder detail = new StringBuilder();
        detail.append("운송장 번호: ").append(parcel.trackingNumber).append('\n');
        detail.append("수령인: ").append(parcel.receiverName).append('\n');
        detail.append("배송 종류: ").append(parcel.getDeliveryType()).append('\n');
        detail.append("배송비: ").append(parcel.calculateFee()).append("원\n");
        detail.append("접수일: ").append(parcel.registeredDate).append('\n');
        detail.append("예상 도착일: ").append(parcel.expectedDeliveryDate).append('\n');
        detail.append("상태: ").append(parcel.status);
        return detail.toString();
    }

    // 택배 한 건의 배송 이력을 문자열로 정리해 반환한다.
    String getHistoryText(String trackingNumber) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        StringBuilder historyText = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int index = 0; index < parcel.historyCount; index++) {
            DeliveryHistory history = parcel.histories[index];
            historyText.append(history.changedAt.format(formatter));
            historyText.append(" / ").append(history.beforeStatus);
            historyText.append(" → ").append(history.afterStatus).append('\n');
        }

        return historyText.toString();
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    Parcel findParcel(String trackingNumber) throws ParcelException {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }
        return parcel;
    }

    // 배송 종류에 맞는 택배 객체를 만든다.
    Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination, int weight, String deliveryType,
                        LocalDate registeredDate) {
        if (deliveryType.equals("특급")) {
            return new ExpressParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }
        if (deliveryType.equals("냉장")) {
            return new RefrigeratedParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }
        if (deliveryType.equals("해외")) {
            return new OverseasParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }
        return new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
    }
}

// 택배 업무 처리에 실패했을 때 사용하는 예외 클래스다.
class ParcelException extends Exception {
    // 오류 원인을 예외 메시지로 전달한다.
    ParcelException(String message) {
        super(message);
    }
}

// 택배 저장과 조회 기능을 약속하는 인터페이스다.
interface ParcelRepository {
    // 택배를 저장하거나 갱신한다.
    void save(Parcel parcel);

    // 운송장 번호로 택배를 찾는다.
    Parcel findByTrackingNumber(String trackingNumber);
}

// 택배를 배열에 저장하는 구현체다.
class MemoryParcelRepository implements ParcelRepository {
    private Parcel[] parcels = new Parcel[100];
    private int count = 0;

    // 새 택배만 배열에 추가한다.
    public void save(Parcel parcel) {
        if (findByTrackingNumber(parcel.trackingNumber) == null) {
            parcels[count] = parcel;
            count++;
        }
    }

    // 배열에서 운송장 번호가 같은 택배를 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < count; index++) {
            if (parcels[index].trackingNumber.equals(trackingNumber)) {
                return parcels[index];
            }
        }
        return null;
    }
}

// 배송 종류가 공유하는 정보와 날짜를 가진 부모 클래스다.
abstract class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    ParcelStatus status = ParcelStatus.접수;
    LocalDate registeredDate;
    LocalDate expectedDeliveryDate;
    DeliveryHistory[] histories = new DeliveryHistory[20];
    int historyCount = 0;

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDate registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.weight = weight;
        this.registeredDate = registeredDate;
    }

    // ========== CH12 변경 ==========
    // 이력 기록 시 현재 시각을 자동으로 저장한다.
    // =================================
    void addHistory(ParcelStatus beforeStatus, ParcelStatus afterStatus) {
        histories[historyCount] = new DeliveryHistory(beforeStatus, afterStatus, LocalDateTime.now());
        historyCount++;
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 배송 종류 이름을 자식 클래스에 맡긴다.
    abstract String getDeliveryType();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int fee = 3000;
        if (weight >= 3) {
            fee += 2000;
        }
        if (destination.equals("제주")) {
            fee += 3000;
        }
        return fee;
    }
}

// 일반 배송 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 일반 배송 택배를 초기화한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }
    // 일반 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 3;
    }
    // 일반 배송 이름을 반환한다.
    String getDeliveryType() {
        return "일반";
    }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }
    // 특급 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
    // 특급 배송 이름을 반환한다.
    String getDeliveryType() {
        return "특급";
    }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }
    // 냉장 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
    // 냉장 배송 이름을 반환한다.
    String getDeliveryType() {
        return "냉장";
    }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }
    // 해외 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 7;
    }
    // 해외 배송 이름을 반환한다.
    String getDeliveryType() {
        return "해외";
    }
}

// 상태 변경 시각까지 보관하는 배송 이력 클래스다.
class DeliveryHistory {
    ParcelStatus beforeStatus;
    ParcelStatus afterStatus;
    LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(ParcelStatus beforeStatus, ParcelStatus afterStatus, LocalDateTime changedAt) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedAt = changedAt;
    }
}
