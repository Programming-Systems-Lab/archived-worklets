/**
 * A Greeter listens for a prompt and answers with a greeting.
 */
public interface Greeter{
  /**
   * Gets the prompt the greeter listens for
   * @return prompt: phrase the greeter is supposed to understand
   */
  public String getPrompt();

  /**
   * Changes the prompt the greeter listens for
   * @param prompt: phrase the greeter is supposed to understand
   */
  public void changePrompt(String prompt);

  /**
   * Gets the greeting the greeter replies with
   * @return greeting: phrase the greeter replies with
   */
  public String getGreeting();

  /**
   * Changes the greeting the greeter will reply with
   * @param greeting: phrase that the greeter will reply with
   */
  public void changeGreeting(String greeting);

  /**
   * Initiates a greeting to the other person
   */
  public void initiateGreeting();

  /**
   * Checks to see if the Greeter is currently in discourse with anyone
   */
  public boolean interlocuting();

  /**
   * Stops the current Greeter's discourse
   */
  public void stopInterlocuting();

  /**
   * Starts the Greeter's discourse
   */
  public void startInterlocuting();
}
