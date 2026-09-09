/**
 * Session M3 Part A / B: record header filled by inline completion,
 * with a static {@code fromUser(User)} mapper (Part B).
 */
public record UserDTO(long id, String name, String email, boolean active) {

    /**
     * Maps the field-based {@link User} POJO onto this record.
     *
     * Part B note: the first accepted completion called
     * {@code u.getFullName()} and {@code u.isEnabled()}; neither exists
     * on User. Fixed by hand to the real accessors.
     */
    public static UserDTO fromUser(User u) {
        return new UserDTO(u.getId(), u.getName(), u.getEmail(), u.isActive());
    }

    public static void main(String[] args) {
        User u = new User(42L, "Ada Lovelace", "ada@example.com", true);
        UserDTO dto = fromUser(u);
        System.out.println(u);
        System.out.println(dto);
    }
}
