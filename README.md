**Please find below few bullet points which describing extra work which I would consider important to do
before this project was turned into a production application:**

- Please note that usually money have a currency and fixed number of decimal places.
So I recommend to develop special class for Money object or use existed solution: 
JSR 354 – “Currency and Money” addresses the standardization of currencies and monetary amounts in Java.

- It will be useful to add batch processing to the money transfer functionality. i.e. create additional method 
    **public List<TransferResult> transfer(List<Transfer> transfers){...}**
    The method allow to run many transfers in multithreading mode.
    
- It is needed to clarify from Functional Analyst how many decimal places (cents or portion of cents) we should accept/hold 
and (just because it is a related question) we should clarify "rounding policy" in case of rounding.

- It makes sense to clarify with users of the transfer method - the error processing strategy.
Should we use hierarchy of checked exceptions or do not use the exceptions at all. For example instead of checked exceptions we can use runtime exceptions or error codes.   
