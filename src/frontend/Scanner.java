/**
 * Scanner class for a simple interpreter.
 * 
 * (c) 2020 by Ronald Mak
 * Department of Computer Science
 * San Jose State University
 */
package frontend;

public class Scanner
{
    private Source source;
    
    /**
     * Constructor.
     * @param source the input source.
     */
    public Scanner(Source source)
    {
        this.source = source;
    }
    
    /**
     * Extract the next token from the source.
     * @return the token.
     */
    public Token nextToken()
    {
        // Skip blanks, comments, and other whitespace characters.
        char ch = nextNonblankCharacter();
        
        if (Character.isLetter(ch))     return Token.word(ch, source);
        else if (Character.isDigit(ch)) return Token.number(ch, source);
        else if (ch == '\'')            return Token.characterOrString(ch, source);
        else                            return Token.specialSymbol(ch, source);
    }
    
    /**
     * Skip blanks, comments, and other whitespace characters
     * and return the next nonblank character.
     * @return the next nonblank character.
     */
    private char nextNonblankCharacter()
    {
        char ch = source.currentChar();
        
        while ((ch == '{') || Character.isWhitespace(ch))
        {
            if (ch == '{')
            {
                // Consume characters of the comment.
                while ((ch != '}') && (ch != Source.EOF)) ch = source.nextChar();
            }
            
            ch = source.nextChar();  // consume character
        }
        
        return ch;  // nonblank character
    }
}
