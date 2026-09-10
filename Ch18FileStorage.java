import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 택배의 현재 상태와 배송 이력에 기록할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

public class Ch18FileStorage {
    // 파일 저장소가 프로그램 재실행 뒤에도 데이터를 읽는지 확인한다.
    public static void main(String[] args) {
        try {
            FileParcelRepository parcelRepository = new FileParcelRepository();
            ParcelService parcelService = new ParcelService(parcelRepository);

            if (parcelRepository.findByTrackingNumber("1001") == null) {
                parcelService.register("1001", "홍길동", "010-1111-2222", "서울", 2, "일반");
                parcelService.changeStatus("1001", ParcelStatus.출고);
            } else {
                System.out.println("기존 파일 데이터를 읽었습니다.");
            }

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

    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    void registerSimple(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination) throws ParcelException, IOException {
        register(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, "일반");
    }

    // 새 택배를 접수하고 파일 저장소에 저장한다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType)
            throws ParcelException, IOException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        if (weight > Parcel.MAXIMUM_WEIGHT) {
            throw new ParcelException("택배 무게는 20kg을 넘을 수 없습니다.");
        }

        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, LocalDate.now());
        parcel.setExpectedDeliveryDate(parcel.getRegisteredDate().plusDays(parcel.getExpectedDeliveryDays()));
        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수);
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, ParcelStatus afterParcelStatus)
            throws ParcelException, IOException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus);
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
    }

    // 저장된 모든 택배의 요약 정보를 출력한다.
    void printAllParcels() throws IOException {
        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                    + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    Parcel findParcel(String trackingNumber) throws ParcelException, IOException {
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
            Parcel parcel = new ExpressParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("냉장")) {
            Parcel parcel = new RefrigeratedParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("해외")) {
            Parcel parcel = new OverseasParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        Parcel parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
        return parcel;
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
        if (!parcelMap.containsKey(parcel.getTrackingNumber())) {
            parcelList.add(parcel);
            parcelMap.put(parcel.getTrackingNumber(), parcel);
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
    // ========== CH18 변경 ==========
    // 프로그램을 다시 실행해도 기존 데이터를 사용할 수 있도록 파일 내용을 객체로 복원한다.
    // =================================
    private void load() throws IOException {
        if (Files.exists(PARCEL_FILE)) {
            for (String line : Files.readAllLines(PARCEL_FILE)) {
                String[] values = line.split("\\t", -1);
                if (values.length != 10) {
                    continue;
                }

                Parcel parcel = createParcel(values[0], values[1], values[2], values[3],
                        Integer.parseInt(values[4]), values[5], LocalDate.parse(values[7]));
                parcel.setParcelStatus(ParcelStatus.valueOf(values[6]));
                parcel.setExpectedDeliveryDate(LocalDate.parse(values[8]));
                parcelList.add(parcel);
                parcelMap.put(parcel.getTrackingNumber(), parcel);
            }
        }

        if (Files.exists(HISTORY_FILE)) {
            for (String line : Files.readAllLines(HISTORY_FILE)) {
                String[] values = line.split("\\t", -1);
                if (values.length != 4) {
                    continue;
                }

                Parcel parcel = parcelMap.get(values[0]);
                if (parcel != null) {
                    parcel.getHistories().add(new DeliveryHistory(ParcelStatus.valueOf(values[1]), ParcelStatus.valueOf(values[2]),
                            LocalDateTime.parse(values[3])));
                }
            }
        }
    }

    // 메모리에 있는 택배와 이력을 각각의 파일에 저장한다.
    // ========== CH18 변경 ==========
    // 메모리의 객체 목록을 택배 파일과 이력 파일에 나누어 저장한다.
    // =================================
    private void writeAll() throws IOException {
        List<String> parcelLines = new ArrayList<>();
        List<String> historyLines = new ArrayList<>();

        for (Parcel parcel : parcelList) {
            parcelLines.add(String.join("\t",
                    parcel.getTrackingNumber(),
                    parcel.getReceiverName(),
                    parcel.getReceiverPhoneNumber(),
                    parcel.getDestination(),
                    String.valueOf(parcel.getWeight()),
                    parcel.getDeliveryType(),
                    parcel.getParcelStatus().toString(),
                    parcel.getRegisteredDate().toString(),
                    parcel.getExpectedDeliveryDate().toString(),
                    String.valueOf(parcel.calculateFee())
            ));

            for (DeliveryHistory history : parcel.getHistories()) {
                historyLines.add(String.join("\t",
                        parcel.getTrackingNumber(),
                        history.getBeforeParcelStatus().toString(),
                        history.getAfterParcelStatus().toString(),
                        history.getChangedAt().toString()
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
        if (deliveryType.equals("특급")) {
            Parcel parcel = new ExpressParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("냉장")) {
            Parcel parcel = new RefrigeratedParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("해외")) {
            Parcel parcel = new OverseasParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        Parcel parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
        return parcel;
    }
}

// 택배 업무 처리에 실패했을 때 사용하는 예외 클래스다.
class ParcelException extends Exception {
    // 오류 원인을 예외 메시지로 전달한다.
    ParcelException(String message) {
        super(message);
    }
}

// 배송 종류가 공유하는 정보와 배송 이력을 가진 부모 클래스다.
abstract class Parcel {
    // ========== CH06 캡슐화 유지 ==========
    // 이후 챕터에서도 운송장 번호와 접수일은 바꾸지 않고, 배송비 규칙은 상수로 관리한다.
    // =================================
    static final int MAXIMUM_WEIGHT = 20;
    private static final int BASIC_DELIVERY_FEE = 3000;
    private static final int HEAVY_PARCEL_SURCHARGE = 2000;
    private static final int JEJU_SURCHARGE = 3000;
    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private int weight;
    private ParcelStatus parcelStatus = ParcelStatus.접수;
    private final LocalDate registeredDate;
    private LocalDate expectedDeliveryDate;
    private List<DeliveryHistory> histories = new ArrayList<>();

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDate registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        setWeight(weight);
        this.registeredDate = registeredDate;
    }

    // 상태 변경 이력을 목록에 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus) {
        histories.add(new DeliveryHistory(beforeParcelStatus, afterParcelStatus, LocalDateTime.now()));
    }

    // 운송장 번호를 반환한다.
    String getTrackingNumber() {
        return trackingNumber;
    }

    // 수령인 이름을 반환한다.
    String getReceiverName() {
        return receiverName;
    }

    // 수령인 연락처를 반환한다.
    String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    // 배송 지역을 반환한다.
    String getDestination() {
        return destination;
    }

    // 택배 무게를 반환한다.
    int getWeight() {
        return weight;
    }

    // 현재 배송 상태를 반환한다.
    ParcelStatus getParcelStatus() {
        return parcelStatus;
    }

    // 업무 처리 결과에 따라 배송 상태를 바꾼다.
    void setParcelStatus(ParcelStatus parcelStatus) {
        this.parcelStatus = parcelStatus;
    }

    // 접수일을 반환한다.
    LocalDate getRegisteredDate() {
        return registeredDate;
    }

    // 예상 도착일을 반환한다.
    LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    // 예상 도착일을 저장한다.
    void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    // 택배 무게를 저장한다. 20kg을 넘으면 택배를 만들 수 없다.
    void setWeight(int weight) {
        this.weight = weight;
    }

    // 배송 이력 목록을 반환한다.
    List<DeliveryHistory> getHistories() {
        return histories;
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 배송 종류 이름을 자식 클래스에 맡긴다.
    abstract String getDeliveryType();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int deliveryFee = BASIC_DELIVERY_FEE;
        if (weight >= 3) {
            deliveryFee += HEAVY_PARCEL_SURCHARGE;
        }
        if (destination.equals("제주")) {
            deliveryFee += JEJU_SURCHARGE;
        }
        return deliveryFee;
    }
}

// 일반 배송 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, LocalDate registeredDate) {
        this(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, registeredDate);
    }

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
    private ParcelStatus beforeParcelStatus;
    private ParcelStatus afterParcelStatus;
    private LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    // 변경 전 상태를 반환한다.
    ParcelStatus getBeforeParcelStatus() {
        return beforeParcelStatus;
    }

    // 변경 후 상태를 반환한다.
    ParcelStatus getAfterParcelStatus() {
        return afterParcelStatus;
    }

    // 변경 시각을 반환한다.
    LocalDateTime getChangedAt() {
        return changedAt;
    }

    DeliveryHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, LocalDateTime changedAt) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.changedAt = changedAt;
    }
}
