# PROMPTS.md — Module 3 hands-on (CS5013)

## How this was run

The handout assumes Copilot ghost text in VS Code. I ran the session through
Claude Code, so nothing showed up as grey overlay text I could accept with Tab.
What's recorded below is what the assistant produced for exactly the context
each part describes.

The Part B failure is real and not reconstructed from memory. I saved the bad
mapper to disk and compiled it, and the javac output below is pasted from that
run.

## Part A: ghost text warm-up

With only this in the buffer:

```java
public record UserDTO(
```

I got:

```java
public record UserDTO(Long id, String username, String email) {
}
```

Then I opened `User.java` in a second tab, deleted the trailing `(` and retyped
it to re-trigger. Second suggestion:

```java
public record UserDTO(long id, String name, String email, boolean active) {
}
```

Four things moved. Three components became four. `Long` became `long`, matching
the field. The invented `username` became the real `name`. And `active` turned
up, presumably read off `isActive()`.

The first version is the user DTO from every tutorial on the internet. The
second one had actually read the file. My keystrokes were identical both times;
the only variable was which tab was open.

## Part B: mapper boilerplate

I typed the stub:

```java
public static UserDTO fromUser(User u) {
    // let the assistant fill in
}
```

and accepted:

```java
public static UserDTO fromUser(User u) {
    return new UserDTO(u.getId(), u.getFullName(), u.getEmail(), u.isEnabled());
}
```

It doesn't compile:

```
UserDTO.java:4: error: cannot find symbol
        return new UserDTO(u.getId(), u.getFullName(), u.getEmail(), u.isEnabled());
                                       ^
  symbol:   method getFullName()
  location: variable u of type User
UserDTO.java:4: error: cannot find symbol
        return new UserDTO(u.getId(), u.getFullName(), u.getEmail(), u.isEnabled());
                                                                      ^
  symbol:   method isEnabled()
  location: variable u of type User
2 errors
```

Two hallucinated methods, `getFullName()` and `isEnabled()`. The real accessors
are `getName()` and `isActive()`. Both guesses are what an average `User` class
on GitHub exposes, which is why they slid past me when I read the line.

Worth noticing what the completion got right: four arguments, correct order,
correct types. Everything it could see was fine and the two names it had to
recall were wrong.

Fixed by hand, no re-prompt:

```java
public static UserDTO fromUser(User u) {
    return new UserDTO(u.getId(), u.getName(), u.getEmail(), u.isActive());
}
```

The `main` I added prints:

```
User{id=42, name=Ada Lovelace, email=ada@example.com, active=true}
UserDTO[id=42, name=Ada Lovelace, email=ada@example.com, active=true]
```

## Part C: controller autofill

I skipped the annotations the handout asks for, and that needs explaining. The
starter isn't a Spring project. `OrderController.java` imports `java.util.*` and
nothing else, and the Makefile builds with plain javac against `libs/junit.jar`.
Typing `@GetMapping` gives you `cannot find symbol` before any test can run. So
I completed the stubs as ordinary methods, the way the hand-written
`listOrders()` is written, and left the HTTP contract in the Javadoc where the
starter already had it.

`getOrderById` came out as:

```java
public Order getOrderById(long id) {
    return store.get(id);
}
```

Accepted unchanged. It reads `store` directly like the hand-written endpoint,
and `Map.get` already returns null on a miss, which is what the Javadoc
promises.

`createOrder`:

```java
public Order createOrder(String item, int qty) {
    if (item == null || item.isBlank()) {
        throw new IllegalArgumentException("item must not be blank");
    }
    if (qty <= 0) {
        throw new IllegalArgumentException("qty must be positive");
    }
    Order order = new Order(nextId++, item, qty);
    store.put(order.id(), order);
    return order;
}
```

Reading it before accepting: returns `Order`, like `listOrders()` hands back
domain objects rather than a `ResponseEntity`. No status codes, correctly, since
there's no HTTP layer here. A completion reaching for
`ResponseEntity.status(201)` would have told me it was pattern-matching "Spring
controller" instead of reading the file. Ids come from `nextId++` as in the
constructor, so a created order can't collide with the seed.

Build and tests:

```
javac --release 17 -d build -cp libs/junit.jar src/*.java test/*.java   # OK
java -jar libs/junit.jar --class-path build --scan-class-path
```

```
[         4 tests found           ]
[         4 tests successful      ]
[         0 tests failed          ]
```

Three of those four fail on a fresh checkout.

## Part D: AI-assisted git

What the assistant drafted:

```
feat: implement UserDTO record and OrderController endpoints

- Add fields id, name, email, active to UserDTO record
- Add static fromUser mapper from User to UserDTO
- Add main method to UserDTO for demonstration
- Implement getOrderById to look up orders in the store
- Implement createOrder with validation and id allocation
- All tests now pass
```

What I committed:

```
feat: complete UserDTO mapper and the two order endpoints

Fill in the record components, the fromUser mapper and the two TODO
stubs so the three failing tests in OrderControllerTest pass.

Written via editor autocompletion as the M3 exercise asks, which is
why the mapper is worth a second look: the first accepted completion
called u.getFullName() and u.isEnabled(), neither of which exists on
User. The compile step caught it, not review. The call had the right
arity, order and types, so it read as correct. That's why I compiled
and ran the endpoints before committing instead of after.

The controller stubs are plain methods, not @GetMapping/@PostMapping:
this starter has no Spring on the classpath, so the annotations the
handout suggests would not compile.
```

The draft subject is true but fits almost any commit on this branch. The body is
worse: an inventory of the diff, one bullet per hunk, all of it already in `git
diff`. Nothing says why. Not that a hallucinated accessor got caught at compile
time, not why I dropped the annotations. "All tests now pass" is the closest it
comes to intent, and that's a result rather than a reason.

I kept the `feat:` prefix, which is honest here: there was a `throw` before and
there's behaviour now. After finding one bad completion the reflex is to
distrust the whole generated message, and that reflex is wrong too.

The pull request is at https://github.com/rikire/m3-hands-on/pull/1, and its
description is the edited version below.

The drafted summary:

```
## Summary
This PR implements the missing functionality in the M3 hands-on starter.

## Changes
- UserDTO: added record components (id, name, email, active)
- UserDTO: added fromUser mapper and a main method
- OrderController: implemented getOrderById
- OrderController: implemented createOrder with input validation

## Testing
All 4 tests in OrderControllerTest pass.
```

Edited:

```
## Summary
Completes the M3 starter: the UserDTO record, its fromUser mapper, and
the two TODO endpoints in OrderController. A fresh checkout has 1 of 4
tests passing; this branch has 4 of 4.

## Changes
- `UserDTO`: record components matching the User POJO's fields
  one-for-one, plus `fromUser` and a small `main` that prints both
  objects.
- `OrderController.getOrderById`: `store.get(id)`, returning null on a
  miss as the existing Javadoc already promised.
- `OrderController.createOrder`: rejects a blank item and a
  non-positive qty, then allocates via `nextId++` so a created order
  can't collide with the seed.
- `PROMPTS.md`: session notes, the deliverable for this exercise.

## Deliberately not done
Part C asks for @GetMapping / @PostMapping. This starter has no Spring
dependency (plain javac, libs/junit.jar), so those annotations don't
compile. The HTTP contract stays in the Javadoc, where the hand-written
listOrders() already keeps it.

## Testing
`make deps && make test`: 4 tests found, 4 successful, 0 failed. Also
ran `java -cp build UserDTO` to exercise the mapper end to end.
```

Same failure mode as the commit message, and I had to add the same kinds of
things by hand. The 1-of-4 to 4-of-4 baseline matters, because "all 4 tests
pass" means nothing to a reviewer who doesn't know that three of them used to
fail. The "deliberately not done" section is invisible in a diff by definition.
And each change needed its reason, not its name.

## Part E: branch name

Prompt:

```
Suggest a branch name for this issue: "customer wants to be able to
close their account permanently". Format: prefix/short-kebab-slug.
Prefix is one of: feat, fix, chore, docs, refactor.
```

Suggestion:

```
feat/permanent-account-closure
```

Close to what I'd pick. The prefix is right, since this is new capability
rather than a repair. I'd have written `feat/close-account-permanently`,
because the issue is phrased as something a user does and a verb-led slug reads
better in `git branch` output than the noun phrase. Small stylistic difference,
not an error. Either name survives review and neither drops "permanently",
which is the word carrying the weight in that issue.

## What I take away from this

The split was clean. The assistant was dependable on structure and unreliable on
anything living in another file. It got the mapper's arity, argument order and
types right unprompted, read `store` directly in `getOrderById` instead of
inventing a repository layer, reused `nextId++` so ids wouldn't collide.

Then it invented `getFullName()`, `isEnabled()` and `username`, each time the
most common version of a real thing. That's the dangerous shape: correct
everywhere it could see, wrong only in the names it had to recall, so the
surrounding correctness vouches for the mistake and rereading the line doesn't
help. Part A is the clean experiment. The one variable I changed was whether
`User.java` was open, and it moved the arity, a type and a field name.

So compiling turned out to be less of a formality than I'd treated it. It caught
both hallucinations in a second and named them exactly, which staring at the
diff would not have done.

Intent is the other gap, and more context wouldn't close it. The commit message
and PR summary were accurate and thin in the same way: they described what the
diff contained, because that's all a diff contains. Why Part C skips the
annotations, why I ran the tests before committing, that three of four used to
fail. None of it is recoverable from changed lines. The assistant sees what
changed; only I know why. Good first draft, dishonest final one.

Rule I'm keeping: let it write the shape, check the names against the file,
write the why myself.
