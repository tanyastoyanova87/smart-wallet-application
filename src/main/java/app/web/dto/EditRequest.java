package app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
public class EditRequest {

    @Size(max = 20, message = "First name cannot be more than 20 symbols")
    private String firstName;

    @Size(max = 20,  message = "Last name cannot be more than 20 symbols")
    private String lastName;

    @Email(message = "Email must be in valid format")
    private String email;

    @URL(message = "Profile picture must be valid web link")
    private String profilePicture;
}
