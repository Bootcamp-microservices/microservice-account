package com.bootcamp.service;

import com.bootcamp.clients.TransactionClientRest;
import com.bootcamp.dto.AccountDto;
import com.bootcamp.dto.Credit;
import com.bootcamp.dto.Operation;
//import com.bootcamp.dto.Credit;
import com.bootcamp.dto.Transaction;
import com.bootcamp.dto.TransactionRest;
import com.bootcamp.entity.Account;
import com.bootcamp.repository.AccountRepository;


import com.bootcamp.util.UtilMethods;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Objects;
import java.util.Optional;


@Service
public class AccountServiceImpl implements AccountService {
	
	private static Logger log = Logger.getLogger(AccountServiceImpl.class);
	
	@Autowired
	private TransactionClientRest clientRest;
	

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public Flux<Account> getAllByIdClient(ObjectId id) {

        Flux<Account> accountFlux = accountRepository.findAllByIdClient(id);
        return accountFlux;
    }


    @Override
    public Mono<Account> savePersonal(AccountDto accountDto) {
        Optional<Account> existsAccount = accountRepository.findByTypeAccount(accountDto.getTypeAccount());
        existsAccount.ifPresent(e -> {

        });
        return null;
    }

    @Override
    public Mono<Account> saveEmpresarial(AccountDto accountDto) {
        return null;
    }

    @Override
    public Mono<Account> saveTypeClient(AccountDto accountDto) {

        switch (accountDto.getTypeClient()) {
            case "Personal":

                Flux<Account> accountFlux = getAllByIdClient(accountDto.getIdClient())
                        .filter(x -> x.getTypeAccount().contains(accountDto.getTypeAccount()));

                Account accountFlux1 = accountFlux.blockFirst();

                if (Objects.isNull(accountFlux1)) {
                    Account account = new Account();
                    account.setIdClient(accountDto.getIdClient());
                    account.setTypeAccount(accountDto.getTypeAccount());
                    account.setNumberAccount(accountDto.getNumberAccount());
                    account.setBalance(accountDto.getBalance());
                    account.setLimit(accountDto.getLimit());
                    account.setDebt(accountDto.getDebt());
                    account.setNumMaxTrans(0);
                    return accountRepository.save(account);
                } else if (accountFlux1.getTypeAccount().equals(accountDto.getTypeAccount())) {
                    return Mono.error(new Exception("CLIENTE YA TIENE UNA CUENTA: " + accountDto.getTypeAccount()));
                }
                break;
            case "Empresarial":
                if (accountDto.getTypeAccount().equals("Corriente")) {
                    Account account = new Account();
                    account.setIdClient(accountDto.getIdClient());
                    account.setTypeAccount(accountDto.getTypeAccount());
                    account.setNumberAccount(accountDto.getNumberAccount());
                    account.setBalance(accountDto.getBalance());
                    account.setLimit(accountDto.getLimit());
                    account.setDebt(accountDto.getDebt());
                    account.setNumMaxTrans(0);
                    return accountRepository.save(account);
                } else {
                    return Mono.error(new Exception("CLIENTE EMPRESARIAL NO PUEDE TENER CUENTAS DE: " + accountDto.getTypeAccount()));

                }
            default:
                return null;
        }
            return null;
    }

    @Override
    public Mono<Account> save(Account account) {
        return accountRepository.save(account);
    }

    @Override
    public Mono<Account> findByIdClientAndTypeAccount(ObjectId id, String typeAccount) {
        return accountRepository.findByIdClient(id).filter(x -> x.getTypeAccount().startsWith(typeAccount));
//        return accountRepository.findByIdClient(id);
    }
    
    @Override
    public Mono<Account> saveTransactionCredit(AccountDto numberAccount, Double amount){
		Mono<Account> account = getAccount(numberAccount);
		Account accot = account.block();
		Double debt =accot.getDebt()+amount;
		if (debt>accot.getLimit()) {
			return account;
		}else {
			accot.setDebt(debt);
			return accountRepository.save(accot);
		}
    }
    
  
    
    @Override
    public Mono<Account> saveTransactionRetirement(AccountDto numberAccount, Double amount){

    	boolean commission = false;
		Mono<Account> account = getAccount(numberAccount);
		Account accot = account.block();
		 Double total= accot.getBalance()-amount;
		 if (total<0) {
			 return account;
		 }else {
			 //Inicio - Cobro de comisiones 
			 if(accot.getNumMaxTrans()>=5) {
				 commission=true;
				 total= accot.getBalance()-(amount-0.10); 
			 }
			 //Fin - Cobro de comisiones 
			 accot.setBalance(total);
			 accot.setNumMaxTrans(accot.getNumMaxTrans()+1);
			 
			 TransactionRest objRest = new TransactionRest();
			 Operation objOperation = new Operation();
			 objOperation.setCodOperation(2);
			 objOperation.setDescription("Retiro");
		     objRest.setSourceAccount(accot.getNumberAccount());
		     objRest.setAmount(amount);
		     objRest.setCommission(commission==true ? 0.10 :0);
		     objRest.setOperation(objOperation);
		     
			return  accountRepository.save(accot);
        }
    }

    @Override
    public Mono<Account> saveTransactionDeposit(AccountDto numberAccount, Double amount){
    
    	boolean commission = false;
    	Mono<Account> account = getAccount(numberAccount);
    	Account accot = account.block();
		 //Deposito
		 System.out.println("Entro 1");
		 Double total= accot.getBalance()+amount;
		 //Inicio - Cobro de comisiones 
		 if(accot.getNumMaxTrans()>=5) {
			  commission=true;
			  total= accot.getBalance()+(amount-0.10);
		 }
		 //Fin - Cobro de comisiones 
		 accot.setBalance(total);
		 accot.setNumMaxTrans(accot.getNumMaxTrans()+1);
		 
		 TransactionRest objRest = new TransactionRest();
		 Operation objOperation = new Operation();
		 objOperation.setCodOperation(1);
		 objOperation.setDescription("Deposito");
	     objRest.setSourceAccount(accot.getNumberAccount());
	     objRest.setAmount(amount);
	     objRest.setCommission(commission==true ? 0.10 :0);
	     objRest.setOperation(objOperation);
	     
	     clientRest.save(objRest).subscribe();
	     
		 return accountRepository.save(accot);
    }

    public Mono<Account> saveTransactionTransfer(AccountDto numberAccount, Double amount,String sourceAccount){
    	Mono<Account> account = getAccount(numberAccount);
    	Account accot = account.block();
		 //Transferencia
		 System.out.println("Entro 3");
		 Double total= accot.getBalance()-amount;
		 if (total<0) {
			 return account;
		 }else {
			 //Inicio - Cobro de comisiones 
			 if(accot.getNumMaxTrans()>=5) {
				 System.out.println("Entro comsinoes");
				 total= accot.getBalance()-(amount-0.10); 
			 }
			 System.out.println("Saldo actual:" +total );
			 //Fin - Cobro de comisiones 
			 accot.setBalance(total);
			 accot.setNumMaxTrans(accot.getNumMaxTrans()+1);

			 //Inicio tranferencia
			 Mono<Account> sourceaMono = getAccountbyNumAccount(sourceAccount);
			 Account sourceaccot = sourceaMono.block();
			 Double sourcetotal= sourceaccot.getBalance()+amount;
			 sourceaccot.setBalance(sourcetotal);
			 //Fin transferencia
			
			 return accountRepository.save(accot).mergeWith(accountRepository.save(sourceaccot)).next();	
		 }
    }

	@Override
	public Mono<Account> saveAccountForYanki(Account account) {
		account.setTypeAccount("Ahorro");
		account.setNumberAccount(UtilMethods.generateNumberAccount(14));
		account.setNumberAccountInterbank(UtilMethods.generateNumberAccount(20));
		account.setWalletYanki(true);
		account.setStatus("true");
		account.setBalance(0.00);
		return accountRepository.save(account);
	}

	@Override
	public Mono<Account> saveTransaction(Transaction transaction) {
		
		Mono<Account> account = getAccount(transaction.getAccountDto());
		
		Account accot = account.block();
		if(accot.getTypeAccount().equals("Credito")) {
			 return saveTransactionCredit(transaction.getAccountDto(),transaction.getAmount());
		}else {
		 if(transaction.getOperation()==1) {
			 return saveTransactionDeposit(transaction.getAccountDto(),transaction.getAmount());

		 }else if (transaction.getOperation()==2) {	 
			 return saveTransactionRetirement(transaction.getAccountDto(),transaction.getAmount());
		 }else {
			 return  saveTransactionTransfer(transaction.getAccountDto(),transaction.getAmount(),transaction.getSourceAccount());
		  }
		 
		}
	
	}


	@Override
	public Mono<Account> updateAccount(Account account) {
        return accountRepository.save(account);
	}


	@Override
	public Mono<Account> getAccount(AccountDto accountDto) {
		return getAllByIdClient(accountDto.getIdClient()).filter(x -> x.getNumberAccount().equals(accountDto.getNumberAccount())).next();
	}


	@Override
	public Mono<Account> getBalanceByAccount(ObjectId idCli, String numberAccount) {
		return getAllByIdClient(idCli).filter(x -> x.getNumberAccount().equals(numberAccount)).next();
	}




	@Override
	public Mono<Account> getAccountbyNumAccount(String numaccountDto) {
		return accountRepository.findAll().filter(x -> x.getNumberAccount().equals(numaccountDto)).next();
	}


	@Override
	public Mono<Account> payCredicAccount(Credit  credit) {
		
		Mono<Account> account = getBalanceByAccount(credit.getIdClient(),credit.getNumberAccount());
		Account accot = account.block();
		System.out.println("lo que debe: "+accot.getDebt());
		accot.setDebt(accot.getDebt()-credit.getMoneyPay());
		return accountRepository.save(accot).mergeWith(saveTransactionRetirement(credit.getAccountDto(),credit.getMoneyPay())).next();
	}


	@Override
	public Mono<Account> findPrimaryAccount(ObjectId id) {
		System.out.println("el id :"+id);
		return getAllByIdClient(id).filter(x->x.getClassification().equals("primary")).next();
	}


	@Override
	public Mono<Account> findAcountByIdClient(ObjectId id) {
		System.out.println("el id findAcountByIdClient:"+id);
		return getAllByIdClient(id).next();
	}


}