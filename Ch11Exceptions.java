public class Ch11Exceptions {
    // 예외 처리 전후를 확인할 간단한 실행 예시다.
    public static void main(String[] args) {
        ParcelService parcelService = new ParcelService(new MemoryParcelRepository());

        try {
            parcelService.register("1001", "홍길동", "010-1111-2222", "서울", 2,
                    "특급", "2026-09-01");
            parcelService.changeStatus("1001", "출고", "2026-09-01");
            parcelService.changeStatus("1001", "취소", "2026-09-01");
        } catch (ParcelException exception) {
            System.out.println(exception.getMessage());
        }
    }
}

// 택배 업무 규칙을 처리하는 클래스다.
class ParcelService {
    private ParcelRepository parcelRepository;

    // 업무 처리에 사용할 저장소를 받는다.
    ParcelService(ParcelRepository parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    // 새 택배를 접수하고 최초 이력을 남긴다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType, String registeredDate)
            throws ParcelException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, registeredDate);
        parcel.addHistory("없음", "접수", registeredDate);
        parcelRepository.save(parcel);

        System.out.println("택배 접수: " + parcel.trackingNumber);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, String afterStatus, String changedDate)
            throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);

        if (!parcel.status.equals("접수")) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.status, afterStatus, changedDate);
        parcel.status = afterStatus;
        parcelRepository.save(parcel);
        System.out.println(afterStatus + " 처리: " + parcel.trackingNumber);
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
                        String destination, int weight, String deliveryType, String registeredDate) {
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

// ========== CH11 변경 ==========
// 업무 처리 실패를 예외 클래스로 전달한다.
// =================================
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

// 배송 종류가 공유하는 기본 정보와 기능을 가진 부모 클래스다.
abstract class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    String status = "접수";
    String registeredDate;
    DeliveryHistory[] histories = new DeliveryHistory[20];
    int historyCount = 0;

    // 배송 종류가 공통으로 사용하는 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, String registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.weight = weight;
        this.registeredDate = registeredDate;
    }

    // 상태 변경 이력을 이 택배에 추가한다.
    void addHistory(String beforeStatus, String afterStatus, String changedDate) {
        histories[historyCount] = new DeliveryHistory(beforeStatus, afterStatus, changedDate);
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
                 String destination, int weight, String registeredDate) {
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
                  String destination, int weight, String registeredDate) {
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
                       String destination, int weight, String registeredDate) {
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
                   String destination, int weight, String registeredDate) {
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

// 택배 상태가 바뀐 시점을 기록하는 클래스다.
class DeliveryHistory {
    String beforeStatus;
    String afterStatus;
    String changedDate;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(String beforeStatus, String afterStatus, String changedDate) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedDate = changedDate;
    }
}
