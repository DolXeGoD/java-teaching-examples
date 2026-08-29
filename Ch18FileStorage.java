import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ch18FileStorage {
    // 파일 저장소가 프로그램 재실행 뒤에도 데이터를 읽는지 확인한다.
    public static void main(String[] args) {
        try {
            ParcelService parcelService = new ParcelService(new FileParcelRepository());
            parcelService.register("1001", "홍길동", "010-1111-2222", "서울", 2, "일반");
            parcelService.changeStatus("1001", "출고");
            parcelService.printAllParcels();
        } catch (ParcelException | IOException exception) {
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

    // 새 택배를 접수하고 파일 저장소에 저장한다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType)
            throws ParcelException, IOException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, LocalDate.now());
        parcel.expectedDeliveryDate = parcel.registeredDate.plusDays(parcel.getExpectedDeliveryDays());
        parcel.addHistory("없음", "접수");
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, String afterStatus)
            throws ParcelException, IOException {
        Parcel parcel = findParcel(trackingNumber);
        if (!parcel.status.equals("접수")) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.status, afterStatus);
        parcel.status = afterStatus;
        parcelRepository.save(parcel);
    }

    // 저장된 모든 택배의 요약 정보를 출력한다.
    void printAllParcels() throws IOException {
        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.trackingNumber + " / " + parcel.receiverName
                    + " / " + parcel.getDeliveryType() + " / " + parcel.status);
        }
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    Parcel findParcel(String trackingNumber) throws ParcelException, IOException {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        return parcel;
    }

    // 배송 종류에 맞는 택배 객체를 만든다.
    Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination, int weight, String deliveryType,
                        LocalDate registeredDate) {
        if (deliveryType.equals("특급")) return new ExpressParcel(trackingNumber, receiverName,
                receiverPhoneNumber, destination, weight, registeredDate);
        if (deliveryType.equals("냉장")) return new RefrigeratedParcel(trackingNumber, receiverName,
                receiverPhoneNumber, destination, weight, registeredDate);
        if (deliveryType.equals("해외")) return new OverseasParcel(trackingNumber, receiverName,
                receiverPhoneNumber, destination, weight, registeredDate);
        return new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
    }
}

// 파일 저장과 메모리 저장에 공통으로 필요한 기능을 약속하는 인터페이스다.
interface ParcelRepository {
    // 택배를 저장하거나 갱신한다.
    void save(Parcel parcel) throws IOException;

    // 운송장 번호로 택배를 찾는다.
    Parcel findByTrackingNumber(String trackingNumber) throws IOException;

    // 저장된 모든 택배를 반환한다.
    List<Parcel> findAll() throws IOException;
}

// ========== CH18 변경 ==========
// 택배와 이력을 텍스트 파일에 저장한다.
// =================================
class FileParcelRepository implements ParcelRepository {
    private static final Path PARCEL_FILE = Path.of("parcel_data.txt");
    private static final Path HISTORY_FILE = Path.of("parcel_history.txt");
    private List<Parcel> parcelList = new ArrayList<>();
    private Map<String, Parcel> parcelMap = new HashMap<>();

    // 프로그램 시작 시 기존 파일 데이터를 메모리로 읽는다.
    FileParcelRepository() throws IOException {
        load();
    }

    // 택배를 메모리에 반영한 뒤 두 파일에 다시 저장한다.
    public void save(Parcel parcel) throws IOException {
        if (!parcelMap.containsKey(parcel.trackingNumber)) {
            parcelList.add(parcel);
            parcelMap.put(parcel.trackingNumber, parcel);
        }
        writeAll();
    }

    // Map에서 운송장 번호로 택배를 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        return parcelMap.get(trackingNumber);
    }

    // 전체 택배 목록을 반환한다.
    public List<Parcel> findAll() {
        return parcelList;
    }

    // 두 파일의 내용을 읽어 택배와 이력을 복원한다.
    private void load() throws IOException {
        if (Files.exists(PARCEL_FILE)) {
            for (String line : Files.readAllLines(PARCEL_FILE)) {
                String[] values = line.split("\\t", -1);
                if (values.length != 10) continue;

                Parcel parcel = createParcel(values[0], values[1], values[2], values[3],
                        Integer.parseInt(values[4]), values[5], LocalDate.parse(values[7]));
                parcel.status = values[6];
                parcel.expectedDeliveryDate = LocalDate.parse(values[8]);
                parcelList.add(parcel);
                parcelMap.put(parcel.trackingNumber, parcel);
            }
        }

        if (Files.exists(HISTORY_FILE)) {
            for (String line : Files.readAllLines(HISTORY_FILE)) {
                String[] values = line.split("\\t", -1);
                if (values.length != 4) continue;

                Parcel parcel = parcelMap.get(values[0]);
                if (parcel != null) {
                    parcel.histories.add(new DeliveryHistory(values[1], values[2],
                            LocalDateTime.parse(values[3])));
                }
            }
        }
    }

    // 메모리에 있는 택배와 이력을 각각의 파일에 저장한다.
    private void writeAll() throws IOException {
        List<String> parcelLines = new ArrayList<>();
        List<String> historyLines = new ArrayList<>();

        for (Parcel parcel : parcelList) {
            parcelLines.add(String.join("\t",
                    parcel.trackingNumber,
                    parcel.receiverName,
                    parcel.receiverPhoneNumber,
                    parcel.destination,
                    String.valueOf(parcel.weight),
                    parcel.getDeliveryType(),
                    parcel.status,
                    parcel.registeredDate.toString(),
                    parcel.expectedDeliveryDate.toString(),
                    String.valueOf(parcel.calculateFee())
            ));

            for (DeliveryHistory history : parcel.histories) {
                historyLines.add(String.join("\t",
                        parcel.trackingNumber,
                        history.beforeStatus,
                        history.afterStatus,
                        history.changedAt.toString()
                ));
            }
        }

        Files.write(PARCEL_FILE, parcelLines);
        Files.write(HISTORY_FILE, historyLines);
    }

    // 파일에서 읽은 배송 종류에 맞는 택배 객체를 만든다.
    private Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                                String destination, int weight, String deliveryType,
                                LocalDate registeredDate) {
        if (deliveryType.equals("특급")) return new ExpressParcel(trackingNumber, receiverName,
                receiverPhoneNumber, destination, weight, registeredDate);
        if (deliveryType.equals("냉장")) return new RefrigeratedParcel(trackingNumber, receiverName,
                receiverPhoneNumber, destination, weight, registeredDate);
        if (deliveryType.equals("해외")) return new OverseasParcel(trackingNumber, receiverName,
                receiverPhoneNumber, destination, weight, registeredDate);
        return new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
    }
}

// 택배 업무 처리에 실패했을 때 사용하는 예외 클래스다.
class ParcelException extends Exception {
    // 오류 원인을 예외 메시지로 전달한다.
    ParcelException(String message) { super(message); }
}

// 배송 종류가 공유하는 정보와 배송 이력을 가진 부모 클래스다.
abstract class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    String status = "접수";
    LocalDate registeredDate;
    LocalDate expectedDeliveryDate;
    List<DeliveryHistory> histories = new ArrayList<>();

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

    // 상태 변경 이력을 목록에 추가한다.
    void addHistory(String beforeStatus, String afterStatus) {
        histories.add(new DeliveryHistory(beforeStatus, afterStatus, LocalDateTime.now()));
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
        if (weight >= 3) fee += 2000;
        if (destination.equals("제주")) fee += 3000;
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
    int calculateFee() { return calculateBaseFee(); }
    // 일반 배송 일수를 반환한다.
    int getExpectedDeliveryDays() { return 3; }
    // 일반 배송 이름을 반환한다.
    String getDeliveryType() { return "일반"; }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 특급 배송비를 계산한다.
    int calculateFee() { return calculateBaseFee() + 2000; }
    // 특급 배송 일수를 반환한다.
    int getExpectedDeliveryDays() { return 1; }
    // 특급 배송 이름을 반환한다.
    String getDeliveryType() { return "특급"; }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() { return calculateBaseFee() + 4000; }
    // 냉장 배송 일수를 반환한다.
    int getExpectedDeliveryDays() { return 1; }
    // 냉장 배송 이름을 반환한다.
    String getDeliveryType() { return "냉장"; }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 해외 배송비를 계산한다.
    int calculateFee() { return calculateBaseFee() + 15000; }
    // 해외 배송 일수를 반환한다.
    int getExpectedDeliveryDays() { return 7; }
    // 해외 배송 이름을 반환한다.
    String getDeliveryType() { return "해외"; }
}

// 상태 변경 시각까지 보관하는 배송 이력 클래스다.
class DeliveryHistory {
    String beforeStatus;
    String afterStatus;
    LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(String beforeStatus, String afterStatus, LocalDateTime changedAt) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedAt = changedAt;
    }
}
