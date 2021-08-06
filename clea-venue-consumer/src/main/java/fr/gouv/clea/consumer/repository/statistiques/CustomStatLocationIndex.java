package fr.gouv.clea.consumer.repository.statistiques;

public interface CustomStatLocationIndex<T> {

    <S extends T> S save(S entity);
}
