package mine.imageweb.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
// cấu hình: cho phép tự động chèn thông tin thời gian tạo, thời gian chỉnh sửa thông tin user
public class AuditingConfig {

}
