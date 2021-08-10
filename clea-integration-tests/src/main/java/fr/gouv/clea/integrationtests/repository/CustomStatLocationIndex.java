package fr.gouv.clea.integrationtests.repository;

public interface CustomStatLocationIndex<T> {

    <S extends T> S save(S entity);
}
